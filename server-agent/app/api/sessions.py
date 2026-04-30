import logging
import asyncio
import re
from asyncio import CancelledError
from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

import asyncpg
from fastapi import APIRouter, Depends, HTTPException, Query, Request
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel, field_validator

from app.agent.service import AgentRuntimeService
from app.agent.state import RunStatus
from app.core.config import get_settings
from app.llm.client import build_llm_client
from app.persistence.redis_state import AgentRedisState
from app.persistence.repository import AgentRepository
from app.security import require_internal_token
from app.services import export_intent_service, message_service, run_service, runtime_factory, session_service, stream_service
from app.tools.business_tools import build_business_registry

router = APIRouter(prefix="/api/agent", tags=["agent"], dependencies=[Depends(require_internal_token)])
logger = logging.getLogger(__name__)

_sessions: dict[str, dict[str, Any]] = {}
_memory_candidates: dict[str, dict[str, Any]] = {}
_repositories: dict[str, AgentRepository] = {}
_redis_states: dict[str, AgentRedisState] = {}
DEFAULT_RUN_PERMISSIONS = {
    "monitoring:read",
    "pipeline:read",
    "knowledge:read",
    "equipment:read",
    "task:read",
    "case:read",
    "alarm:read",
    "inspection:read",
    "maintenance:read",
    "emergency:read",
    "log:read",
    "report:read",
}


class CreateSessionRequest(BaseModel):
    raw_input: str | None = None
    title: str | None = None
    source_type: str = "manual"
    source_id: str | None = None
    object_type: str | None = None
    object_id: str | None = None
    object_name: str | None = None

    @field_validator("raw_input", "title")
    @classmethod
    def optional_text_must_not_be_blank(cls, value: str | None) -> str | None:
        if value is None:
            return value
        stripped = value.strip()
        if not stripped:
            raise ValueError("text fields must not be blank")
        return stripped

    def model_post_init(self, __context: Any) -> None:
        if not self.raw_input and not self.title:
            raise ValueError("raw_input or title is required")


class CreateMessageRequest(BaseModel):
    content: str
    message_type: str = "text"

    @field_validator("content")
    @classmethod
    def content_must_not_be_blank(cls, value: str) -> str:
        stripped = value.strip()
        if not stripped:
            raise ValueError("content must not be blank")
        return stripped


@router.post("/sessions")
async def create_session(request: CreateSessionRequest) -> dict[str, str]:
    return await session_service.create_session(
        request,
        use_in_memory_store=_use_in_memory_store,
        get_repository=_get_repository,
        sessions=_sessions,
        now_iso=_now_iso,
        logger=logger,
    )


@router.get("/sessions")
async def list_sessions(limit: int = Query(20, ge=1, le=100)) -> dict[str, Any]:
    return await session_service.list_sessions(
        limit,
        use_in_memory_store=_use_in_memory_store,
        get_repository=_get_repository,
        sessions=_sessions,
    )


@router.delete("/sessions/{session_id}")
async def delete_session(session_id: str) -> dict[str, str]:
    return await session_service.delete_session(
        session_id,
        use_in_memory_store=_use_in_memory_store,
        get_repository=_get_repository,
        get_redis_state=_get_redis_state,
        sessions=_sessions,
    )


@router.post("/sessions/{session_id}/messages")
async def create_message(session_id: str, request: CreateMessageRequest) -> dict[str, Any]:
    return await message_service.create_message(
        session_id,
        request,
        use_in_memory_store=_use_in_memory_store,
        get_repository=_get_repository,
        get_session=_get_session,
        infer_export_plan=_infer_export_plan,
        active_run_response=_active_run_response,
        message_response=_message_response,
        run_created_response=_run_created_response,
        now_iso=_now_iso,
    )


@router.post("/sessions/{session_id}/runs")
async def run_session(session_id: str) -> dict[str, Any]:
    if not _use_in_memory_store():
        return await _run_persistent_session(session_id)

    session = _get_session(session_id)
    if session.get("run_id"):
        raise HTTPException(status_code=409, detail="Session already has a run")

    run_id = f"run_{uuid4().hex}"
    logger.info("agent run started session_id=%s run_id=%s mode=in_memory", session_id, run_id)
    raw_input = session["request"]["raw_input"]
    service = _build_runtime_service(_accepted_summary_memory(raw_input))
    events = [
        event.model_dump(mode="json")
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=raw_input,
            permissions=DEFAULT_RUN_PERMISSIONS,
        )
    ]
    session["status"] = _derive_run_status(events)
    session["run_id"] = run_id
    session["events"] = events
    if events:
        await _get_redis_state().remember_last_seq(session_id, events[-1]["seq"])
    logger.info("agent run finished session_id=%s run_id=%s status=%s events=%s", session_id, run_id, session["status"], len(events))
    return {"session_id": session_id, "run_id": run_id, "events": events}


@router.get("/sessions/{session_id}/runs/stream")
async def stream_run_session(
    session_id: str,
    request: Request,
    after_seq: int = Query(0, alias="afterSeq"),
) -> StreamingResponse:
    if not _use_in_memory_store():
        return StreamingResponse(
            _stream_persistent_session(session_id, request, after_seq),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    session = _get_session(session_id)
    if session.get("run_id"):
        raise HTTPException(status_code=409, detail="Session already has a run")

    run_id = f"run_{uuid4().hex}"
    session["run_id"] = run_id
    session["status"] = "running"
    logger.info("agent stream run started session_id=%s run_id=%s mode=in_memory", session_id, run_id)

    async def generator():
        service = _build_runtime_service(_accepted_summary_memory(session["request"]["raw_input"]))
        events = []
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=session["request"]["raw_input"],
            permissions=DEFAULT_RUN_PERMISSIONS,
        ):
            if await _get_redis_state().is_cancelled(run_id):
                session["status"] = "cancelled"
                yield _sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": "cancelled"})
                return
            if await request.is_disconnected():
                session["status"] = "cancelled"
                await _get_redis_state().request_cancel(run_id)
                logger.info("agent stream client disconnected session_id=%s run_id=%s mode=in_memory events=%s", session_id, run_id, len(events))
                return
            payload = event.model_dump(mode="json")
            _log_agent_event(payload)
            events.append(payload)
            session["events"] = events
            await _get_redis_state().remember_last_seq(session_id, event.seq)
            yield _sse_event("agent_event", payload)
        session["status"] = _derive_run_status(events)
        logger.info("agent stream run finished session_id=%s run_id=%s status=%s events=%s", session_id, run_id, session["status"], len(events))
        yield _sse_event(
            "run_status",
            {"session_id": session_id, "run_id": run_id, "status": session["status"]},
        )

    return StreamingResponse(
        generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.get("/sessions/{session_id}/runs/{run_id}/stream")
async def stream_specific_run_session(
    session_id: str,
    run_id: str,
    request: Request,
    after_seq: int = Query(0, alias="afterSeq"),
) -> StreamingResponse:
    if not _use_in_memory_store():
        return StreamingResponse(
            _stream_persistent_specific_run(session_id, run_id, request, after_seq),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    session = _get_session(session_id)
    run = session.get("runs", {}).get(run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="Run not found")
    if run["status"] != "created":
        if stream_service.can_replay_status(run.get("status")):
            return StreamingResponse(
                stream_service.replay_events(
                    run.get("events", []),
                    session_id=session_id,
                    run_id=run_id,
                    status=run["status"],
                    after_seq=after_seq,
                ),
                media_type="text/event-stream",
                headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
            )
        raise HTTPException(status_code=409, detail="Run already started")
    run["status"] = "running"
    session["status"] = "running"

    async def generator():
        service = _build_runtime_service(_accepted_summary_memory(run["input_text"]))
        events = []
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=run["input_text"],
            permissions=DEFAULT_RUN_PERMISSIONS,
        ):
            if await _get_redis_state().is_cancelled(run_id):
                run["status"] = "cancelled"
                session["status"] = "cancelled"
                session["active_run_id"] = None
                yield _sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": "cancelled"})
                return
            if await request.is_disconnected():
                run["status"] = "cancelled"
                session["status"] = "cancelled"
                session["active_run_id"] = None
                await _get_redis_state().request_cancel(run_id)
                return
            payload = event.model_dump(mode="json")
            _log_agent_event(payload)
            events.append(payload)
            run["events"] = events
            session["events"] = events
            await _get_redis_state().remember_last_seq(session_id, event.seq)
            yield _sse_event("agent_event", payload)

        final_status = _derive_run_status(events)
        _complete_in_memory_message_run(session, run, events, final_status)
        session["updated_at"] = _now_iso()
        yield _sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": final_status})

    return StreamingResponse(
        generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.get("/sessions/{session_id}/events")
async def list_events(session_id: str, after_seq: int = 0) -> dict[str, Any]:
    if not _use_in_memory_store():
        session = await _get_repository().get_session(session_id)
        if session is None:
            raise HTTPException(status_code=404, detail="Session not found")
        return {
            "session_id": session_id,
            "run_id": session.get("run_id"),
            "events": list(await _get_repository().list_events(session_id, after_seq)),
        }

    session = _get_session(session_id)
    return {
        "session_id": session_id,
        "run_id": session.get("run_id"),
        "events": [event for event in session["events"] if event["seq"] > after_seq],
    }


@router.get("/sessions/{session_id}/runs/{run_id}/events")
async def list_run_events(
    session_id: str,
    run_id: str,
    after_seq: int = Query(0, alias="afterSeq"),
) -> dict[str, Any]:
    if not _use_in_memory_store():
        session = await _get_repository().get_session(session_id)
        if session is None:
            raise HTTPException(status_code=404, detail="Session not found")
        return {
            "session_id": session_id,
            "run_id": run_id,
            "events": list(await _get_repository().list_run_events(session_id, run_id, after_seq)),
        }

    session = _get_session(session_id)
    run = session.get("runs", {}).get(run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="Run not found")
    return {
        "session_id": session_id,
        "run_id": run_id,
        "events": [event for event in run.get("events", []) if event["seq"] > after_seq],
    }


@router.get("/sessions/{session_id}/timeline")
async def list_timeline(
    session_id: str,
    before_cursor: str | None = Query(None, alias="beforeCursor"),
    limit: int = 1,
) -> dict[str, Any]:
    if not _use_in_memory_store():
        session = await _get_repository().get_session(session_id)
        if session is None:
            raise HTTPException(status_code=404, detail="Session not found")
        return _timeline_response(await _get_repository().list_timeline(session_id, before_cursor, limit))

    session = _get_session(session_id)
    return _timeline_response(_list_in_memory_timeline(session, before_cursor, limit))


@router.post("/sessions/{session_id}/runs/{run_id}/cancel")
async def cancel_run(session_id: str, run_id: str) -> dict[str, str]:
    if not _use_in_memory_store():
        session = await _get_repository().get_session(session_id)
        if session is None or session.get("run_id") != run_id:
            raise HTTPException(status_code=404, detail="Run not found")
        await _get_redis_state().request_cancel(run_id)
        await _get_repository().complete_run(session_id, run_id, "cancelled")
        return {"session_id": session_id, "run_id": run_id, "status": "cancel_requested"}

    session = _get_session(session_id)
    if session.get("run_id") != run_id and session.get("active_run_id") != run_id:
        raise HTTPException(status_code=404, detail="Run not found")
    session["status"] = "cancelled"
    session["active_run_id"] = None
    run = session.get("runs", {}).get(run_id)
    if run is not None:
        run["status"] = "cancelled"
    await _get_redis_state().request_cancel(run_id)
    return {"session_id": session_id, "run_id": run_id, "status": "cancel_requested"}


def _get_session(session_id: str) -> dict[str, Any]:
    if session_id not in _sessions:
        raise HTTPException(status_code=404, detail="Session not found")
    return _sessions[session_id]


def _use_in_memory_store() -> bool:
    return get_settings().use_in_memory_store


def _get_repository() -> AgentRepository:
    database_url = get_settings().database_url
    if database_url not in _repositories:
        _repositories[database_url] = AgentRepository.from_database_url(database_url)
    return _repositories[database_url]


def _get_redis_state() -> AgentRedisState:
    settings = get_settings()
    if settings.use_in_memory_store:
        return _redis_states.setdefault("memory", AgentRedisState.in_memory())
    return _redis_states.setdefault(settings.redis_url, AgentRedisState.from_url(settings.redis_url))


def _build_runtime_service(summary_memory: list[str] | None = None) -> AgentRuntimeService:
    return runtime_factory.build_runtime_service(
        summary_memory,
        runtime_cls=AgentRuntimeService,
        registry_factory=build_business_registry,
    )


async def _list_accepted_memories(repository: Any, query: str | None) -> list[str]:
    list_memories = getattr(repository, "list_accepted_memories", None)
    if not callable(list_memories):
        return []
    return await list_memories(query or "")


async def _start_run_if_supported(repository: Any, session_id: str, run_id: str) -> None:
    start_run = getattr(repository, "start_run", None)
    if callable(start_run):
        await start_run(session_id, run_id)


async def _run_persistent_session(session_id: str) -> dict[str, Any]:
    repository = _get_repository()
    session = await repository.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    if session.get("run_id"):
        raise HTTPException(status_code=409, detail="Session already has a run")

    run_id = f"run_{uuid4().hex}"
    await repository.create_run(session_id, run_id)
    await _start_run_if_supported(repository, session_id, run_id)
    logger.info("agent run started session_id=%s run_id=%s mode=persistent", session_id, run_id)
    service = _build_runtime_service(await _list_accepted_memories(repository, session.get("raw_input") or ""))
    events = []
    async for event in service.run_analysis(
        session_id=session_id,
        run_id=run_id,
        raw_input=session["raw_input"],
        permissions=DEFAULT_RUN_PERMISSIONS,
    ):
        await repository.append_event(event)
        payload = event.model_dump(mode="json")
        _log_agent_event(payload)
        events.append(payload)
        await _get_redis_state().remember_last_seq(session_id, event.seq)

    final_status = _derive_run_status(events)
    await repository.complete_run(session_id, run_id, final_status)
    logger.info("agent run finished session_id=%s run_id=%s status=%s events=%s", session_id, run_id, final_status, len(events))
    return {"session_id": session_id, "run_id": run_id, "events": events}


async def _stream_persistent_specific_run(
    session_id: str,
    run_id: str,
    request: Request | None = None,
    after_seq: int = 0,
):
    repository = _get_repository()
    run = await repository.get_run(session_id, run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="Run not found")
    if run["status"] != "created":
        if stream_service.can_replay_status(run.get("status")):
            async for item in stream_service.replay_persistent_run(
                repository,
                session_id=session_id,
                run_id=run_id,
                status=run["status"],
                after_seq=after_seq,
            ):
                yield item
            return
        raise HTTPException(status_code=409, detail="Run already started")

    logger.info("agent stream run started session_id=%s run_id=%s mode=persistent-multiturn", session_id, run_id)
    await _start_run_if_supported(repository, session_id, run_id)
    service = _build_runtime_service(await _list_accepted_memories(repository, run.get("input_text") or ""))
    events: list[dict[str, Any]] = []
    cancelled_by_disconnect = False
    cancelled_by_request = False
    cleanup_scheduled = False
    try:
        context = await repository.list_recent_context(session_id)
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=run["input_text"] or "",
            permissions=DEFAULT_RUN_PERMISSIONS,
            conversation_context=context,
        ):
            if await _get_redis_state().is_cancelled(run_id):
                cancelled_by_request = True
                break
            if request is not None and await request.is_disconnected():
                cancelled_by_disconnect = True
                break
            await asyncio.shield(repository.append_event(event))
            payload = event.model_dump(mode="json")
            _log_agent_event(payload)
            events.append(payload)
            await asyncio.shield(_get_redis_state().remember_last_seq(session_id, event.seq))
            yield _sse_event("agent_event", payload)
    except CancelledError:
        cancelled_by_disconnect = True
        cleanup_scheduled = True
        asyncio.create_task(_complete_cancelled_run_later(session_id, run_id, len(events)))
        raise
    finally:
        if cancelled_by_disconnect and not cleanup_scheduled:
            await _complete_cancelled_run(repository, session_id, run_id, len(events))

    if cancelled_by_disconnect:
        return
    if cancelled_by_request:
        await repository.complete_run(session_id, run_id, "cancelled")
        yield _sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": "cancelled"})
        return
    final_status = _derive_run_status(events)
    await repository.complete_run(session_id, run_id, final_status)
    summary = _build_run_summary(events)
    await repository.create_run_summary(
        summary_id=f"sum_{uuid4().hex}",
        session_id=session_id,
        run_id=run_id,
        summary=summary,
        triggering_message_id=run.get("triggering_message_id"),
    )
    await repository.create_message(
        message_id=f"msg_{uuid4().hex}",
        session_id=session_id,
        role="assistant",
        content=summary.get("final_answer") or summary.get("judgment") or f"本轮研判{final_status}",
        triggered_run_id=run_id,
    )
    await _store_persistent_memory_candidates(repository, session_id, run_id, run.get("input_text") or "", summary)
    logger.info("agent stream run finished session_id=%s run_id=%s status=%s events=%s", session_id, run_id, final_status, len(events))
    yield _sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": final_status})


async def _stream_persistent_session(
    session_id: str,
    request: Request | None = None,
    after_seq: int = 0,
):
    repository = _get_repository()
    session = await repository.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    if session.get("run_id"):
        run = await repository.get_run(session_id, session["run_id"])
        if run is not None and stream_service.can_replay_status(run.get("status")):
            async for item in stream_service.replay_persistent_run(
                repository,
                session_id=session_id,
                run_id=session["run_id"],
                status=run["status"],
                after_seq=after_seq,
            ):
                yield item
            return
        raise HTTPException(status_code=409, detail="Session already has a run")

    run_id = f"run_{uuid4().hex}"
    await repository.create_run(session_id, run_id)
    await _start_run_if_supported(repository, session_id, run_id)
    logger.info("agent stream run started session_id=%s run_id=%s mode=persistent", session_id, run_id)
    service = _build_runtime_service(await _list_accepted_memories(repository, session.get("raw_input") or ""))
    events: list[dict[str, Any]] = []
    cancelled_by_disconnect = False
    cancelled_by_request = False
    cleanup_scheduled = False
    try:
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=session["raw_input"],
            permissions=DEFAULT_RUN_PERMISSIONS,
        ):
            if await _get_redis_state().is_cancelled(run_id):
                cancelled_by_request = True
                break
            if request is not None and await request.is_disconnected():
                cancelled_by_disconnect = True
                break
            await asyncio.shield(repository.append_event(event))
            payload = event.model_dump(mode="json")
            _log_agent_event(payload)
            events.append(payload)
            await asyncio.shield(_get_redis_state().remember_last_seq(session_id, event.seq))
            yield _sse_event("agent_event", payload)
    except CancelledError:
        cancelled_by_disconnect = True
        cleanup_scheduled = True
        asyncio.create_task(_complete_cancelled_run_later(session_id, run_id, len(events)))
        raise
    finally:
        if cancelled_by_disconnect and not cleanup_scheduled:
            await _complete_cancelled_run(repository, session_id, run_id, len(events))

    if cancelled_by_disconnect:
        return
    if cancelled_by_request:
        await repository.complete_run(session_id, run_id, "cancelled")
        yield _sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": "cancelled"})
        return
    final_status = _derive_run_status(events)
    await repository.complete_run(session_id, run_id, final_status)
    logger.info("agent stream run finished session_id=%s run_id=%s status=%s events=%s", session_id, run_id, final_status, len(events))
    yield _sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": final_status})


async def _complete_cancelled_run(repository: AgentRepository, session_id: str, run_id: str, event_count: int) -> None:
    try:
        await _get_redis_state().request_cancel(run_id)
        await _complete_cancelled_run_with_fresh_connection(session_id, run_id)
        logger.info("agent stream client disconnected session_id=%s run_id=%s mode=persistent events=%s", session_id, run_id, event_count)
    except Exception:
        logger.exception("failed to mark disconnected agent stream cancelled session_id=%s run_id=%s", session_id, run_id)


async def _complete_cancelled_run_later(session_id: str, run_id: str, event_count: int) -> None:
    await asyncio.sleep(0.2)
    await _complete_cancelled_run(_get_repository(), session_id, run_id, event_count)


async def _complete_cancelled_run_with_fresh_connection(session_id: str, run_id: str) -> None:
    database_url = get_settings().database_url.replace("postgresql+asyncpg://", "postgresql://", 1)
    connection = await asyncpg.connect(database_url)
    try:
        await connection.execute(
            "UPDATE agent_run SET status = 'cancelled', finished_at = now(), updated_at = now() WHERE id = $1",
            run_id,
        )
        await connection.execute(
            "UPDATE analysis_session SET status = 'cancelled', completed_at = now(), updated_at = now() WHERE id = $1",
            session_id,
        )
    finally:
        await connection.close()


def _derive_run_status(events: list[dict[str, Any]]) -> RunStatus:
    return run_service.derive_run_status(events)


def _active_run_response(session_id: str, run_id: str) -> JSONResponse:
    return JSONResponse(
        status_code=409,
        content={
            "code": "SESSION_RUN_IN_PROGRESS",
            "message": "当前会话仍有研判在运行，请等待完成或取消后继续追问。",
            "sessionId": session_id,
            "runId": run_id,
        },
    )


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _message_response(message: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": message["id"],
        "role": message["role"],
        "content": message["content"],
        "messageType": message.get("message_type", "text"),
        "createdAt": message.get("created_at"),
    }


def _run_created_response(session_id: str, run_id: str) -> dict[str, Any]:
    return {
        "id": run_id,
        "sessionId": session_id,
        "status": "created",
        "streamUrl": f"/manager/virtual-expert/agent/sessions/{session_id}/runs/{run_id}/stream",
    }


async def _infer_export_plan(content: str) -> dict[str, Any] | None:
    return await export_intent_service.infer_export_plan(
        content,
        llm_client_factory=build_llm_client,
    )


def _accepted_summary_memory(query: str | None, limit: int = 5) -> list[str]:
    normalized_query = (query or "").lower()
    scored: list[tuple[int, str]] = []
    for memory in _memory_candidates.values():
        if memory.get("status") != "accepted":
            continue
        content = str(memory.get("content") or "")
        memory_type = str(memory.get("memory_type") or "")
        score = 1 if memory_type in {"preference", "export_preference"} else 0
        for token in re.findall(r"[a-z0-9][a-z0-9_-]{1,}", content.lower()):
            if token in normalized_query:
                score += 2
        if score > 0:
            scored.append((score, f"[{memory_type}] {content}"))
    scored.sort(key=lambda item: item[0], reverse=True)
    return [content for _, content in scored[:limit]]


def _store_in_memory_candidates(session: dict[str, Any], run: dict[str, Any], summary: dict[str, Any]) -> None:
    run_service.store_in_memory_candidates(
        session,
        run,
        summary,
        memory_candidates=_memory_candidates,
        now_iso=_now_iso,
    )


async def _store_persistent_memory_candidates(
    repository: AgentRepository,
    session_id: str,
    run_id: str,
    input_text: str,
    summary: dict[str, Any],
) -> None:
    if not any(word in input_text for word in ("记住", "以后", "默认", "下次")):
        return
    memory_type = "export_preference" if any(word in input_text.lower() for word in ("导出", "pdf", "excel", "报告")) else "preference"
    await repository.create_memory_candidate(
        memory_id=f"mem_{uuid4().hex}",
        session_id=session_id,
        run_id=run_id,
        memory_type=memory_type,
        title=input_text[:80],
        content=_clean_memory_content(input_text),
        status="accepted",
        confidence=0.9,
        evidence_ids=summary.get("key_evidence") if isinstance(summary.get("key_evidence"), list) else None,
    )


def _clean_memory_content(content: str) -> str:
    return run_service.clean_memory_content(content)


def _complete_in_memory_message_run(
    session: dict[str, Any],
    run: dict[str, Any],
    events: list[dict[str, Any]],
    final_status: RunStatus,
) -> None:
    run_service.complete_in_memory_message_run(
        session,
        run,
        events,
        final_status,
        memory_candidates=_memory_candidates,
        now_iso=_now_iso,
    )


def _build_run_summary(events: list[dict[str, Any]]) -> dict[str, Any]:
    return run_service.build_run_summary(events)


def _list_in_memory_timeline(session: dict[str, Any], before_cursor: str | None, limit: int) -> dict[str, Any]:
    limit = max(1, min(limit, 20))
    user_messages = [message for message in session.get("messages", []) if message["role"] == "user"]
    user_messages.sort(key=lambda message: session["messages"].index(message), reverse=True)
    if before_cursor:
        _, cursor_id = before_cursor.split("|", 1)
        cursor_index = next(
            (index for index, message in enumerate(user_messages) if message["id"] == cursor_id),
            None,
        )
        if cursor_index is not None:
            user_messages = user_messages[cursor_index + 1:]
    selected_desc = user_messages[: limit + 1]
    has_more = len(selected_desc) > limit
    selected = selected_desc[:limit]
    selected.reverse()
    items = [_in_memory_timeline_item(session, message) for message in selected]
    return {
        "session_id": session.get("id"),
        "items": items,
        "has_more_before": has_more,
        "before_cursor": items[0]["cursor"] if items else before_cursor,
    }


def _in_memory_timeline_item(session: dict[str, Any], message: dict[str, Any]) -> dict[str, Any]:
    run = next(
        (
            candidate
            for candidate in session.get("runs", {}).values()
            if candidate.get("triggering_message_id") == message["id"]
        ),
        None,
    )
    run_payload = None
    if run:
        run_payload = {
            "id": run["id"],
            "status": run["status"],
            "summary": run.get("summary"),
            "event_count": len(run.get("events", [])),
            "created_at": run.get("created_at"),
            "completed_at": run.get("completed_at"),
        }
    return {
        "turn_id": message["id"],
        "cursor": f"{message.get('created_at')}|{message['id']}",
        "user_message": {
            "id": message["id"],
            "content": message["content"],
            "created_at": message.get("created_at"),
        },
        "run": run_payload,
    }


def _timeline_response(payload: dict[str, Any]) -> dict[str, Any]:
    return {
        "sessionId": payload.get("session_id"),
        "items": [_timeline_item_response(item) for item in payload.get("items", [])],
        "hasMoreBefore": payload.get("has_more_before", False),
        "beforeCursor": payload.get("before_cursor"),
    }


def _timeline_item_response(item: dict[str, Any]) -> dict[str, Any]:
    run = item.get("run")
    return {
        "turnId": item.get("turn_id"),
        "cursor": item.get("cursor"),
        "userMessage": {
            "id": item.get("user_message", {}).get("id"),
            "content": item.get("user_message", {}).get("content"),
            "createdAt": item.get("user_message", {}).get("created_at"),
        },
        "run": None if run is None else {
            "id": run.get("id"),
            "status": run.get("status"),
            "summary": _summary_response(run.get("summary")),
            "eventCount": run.get("event_count", 0),
            "createdAt": run.get("created_at"),
            "completedAt": run.get("completed_at"),
        },
    }


def _summary_response(summary: dict[str, Any] | None) -> dict[str, Any] | None:
    if not summary:
        return None
    return {
        "finalAnswer": summary.get("final_answer"),
        "riskLevel": summary.get("risk_level"),
        "judgment": summary.get("judgment"),
        "recommendedActions": summary.get("recommended_actions", []),
        "keyEvidence": summary.get("key_evidence", []),
        "openQuestions": summary.get("open_questions", []),
    }


def _sse_event(event_name: str, payload: dict[str, Any]) -> str:
    return stream_service.sse_event(event_name, payload)


def _log_agent_event(payload: dict[str, Any]) -> None:
    stream_service.log_agent_event(logger, payload)
