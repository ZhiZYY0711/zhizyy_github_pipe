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
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.agent.events import AgentEvent, AgentEventType
from app.agent.service import AgentRuntimeService
from app.agent.state import RunStatus
from app.core.config import get_settings
from app.llm.model_choice import ModelChoice, resolve_model_choice
from app.persistence.redis_state import AgentRedisState
from app.persistence.repository import AgentRepository
from app.security import require_internal_token
from app.services import (
    message_service,
    preference_memory_service,
    run_service,
    runtime_factory,
    session_service,
    stream_service,
    title_service,
)
from app.tools.business_tools import build_business_registry
from app.tools.http_client import build_tool_http_client

router = APIRouter(prefix="/api/agent", tags=["agent"], dependencies=[Depends(require_internal_token)])
public_router = APIRouter(prefix="/api/agent", tags=["agent-public"])
logger = logging.getLogger(__name__)

_sessions: dict[str, dict[str, Any]] = {}
_shares: dict[str, dict[str, Any]] = {}
_memory_candidates: dict[str, dict[str, Any]] = {}
_user_memories: dict[str, list[dict[str, Any]]] = {}
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
    "operations:read",
    "report:read",
    "report:write",
}


class CreateSessionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    raw_input: str | None = None
    title: str | None = None
    source_type: str = Field("manual", alias="sourceType")
    source_id: str | None = Field(None, alias="sourceId")
    object_type: str | None = Field(None, alias="objectType")
    object_id: str | None = Field(None, alias="objectId")
    object_name: str | None = Field(None, alias="objectName")
    model_tier: str | None = Field(None, alias="modelTier")

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
    model_config = ConfigDict(populate_by_name=True)

    content: str
    message_type: str = Field("text", alias="messageType")
    model_tier: str | None = Field(None, alias="modelTier")

    @field_validator("content")
    @classmethod
    def content_must_not_be_blank(cls, value: str) -> str:
        stripped = value.strip()
        if not stripped:
            raise ValueError("content must not be blank")
        return stripped


class UpdateSessionRequest(BaseModel):
    title: str | None = None
    pinned: bool | None = None
    archived: bool | None = None

    @field_validator("title")
    @classmethod
    def title_must_not_be_blank(cls, value: str | None) -> str | None:
        if value is None:
            return value
        stripped = value.strip()
        if not stripped:
            raise ValueError("title must not be blank")
        return stripped[:200]


class ShareSessionRequest(BaseModel):
    type: str = "link"

    @field_validator("type")
    @classmethod
    def type_must_be_supported(cls, value: str) -> str:
        if value not in {"link", "md"}:
            raise ValueError("type must be link or md")
        return value


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
async def list_sessions(
    limit: int = Query(20, ge=1, le=100),
    archived: bool = Query(False),
) -> dict[str, Any]:
    if archived:
        if not _use_in_memory_store():
            return {"sessions": list(await _get_repository().list_archived_sessions(limit))}
        ordered = sorted(
            [session for session in _sessions.values() if session.get("archived_at")],
            key=lambda session: session.get("archived_at") or "",
            reverse=True,
        )
        return {"sessions": [_in_memory_session_response(session) for session in ordered[:limit]]}
    return await session_service.list_sessions(
        limit,
        use_in_memory_store=_use_in_memory_store,
        get_repository=_get_repository,
        sessions=_sessions,
    )


@router.patch("/sessions/{session_id}")
async def update_session(session_id: str, request: UpdateSessionRequest) -> dict[str, Any]:
    if not _use_in_memory_store():
        session = await _get_repository().update_session(
            session_id,
            title=request.title,
            pinned=request.pinned,
            archived=request.archived,
        )
        if session is None:
            raise HTTPException(status_code=404, detail="Session not found")
        return _session_response(session)

    session = _get_session(session_id)
    if request.title is not None:
        session["title"] = request.title
    if request.pinned is not None:
        session["pinned"] = request.pinned
    if request.archived is not None:
        session["archived_at"] = _now_iso() if request.archived else None
        session["status"] = "archived" if request.archived else "completed"
    session["updated_at"] = _now_iso()
    return _session_response(_in_memory_session_response(session))


@router.post("/sessions/{session_id}/share")
async def share_session(session_id: str, request: ShareSessionRequest) -> dict[str, Any]:
    if not _use_in_memory_store():
        repository = _get_repository()
        session = await repository.get_session(session_id)
        if session is None:
            raise HTTPException(status_code=404, detail="Session not found")
        snapshot = await _persistent_share_snapshot(repository, session_id)
        share_id = f"shr_{uuid4().hex}"
        share = await repository.create_share(
            share_id=share_id,
            session_id=session_id,
            share_type=request.type,
            title=snapshot["session"]["title"],
            snapshot=snapshot,
        )
        return _share_response(share)

    session = _get_session(session_id)
    snapshot = _in_memory_share_snapshot(session)
    share_id = f"shr_{uuid4().hex}"
    share = {
        "id": share_id,
        "session_id": session_id,
        "share_type": request.type,
        "title": snapshot["session"]["title"],
        "snapshot": snapshot,
        "created_at": _now_iso(),
    }
    _shares[share_id] = share
    return _share_response(share)


@public_router.get("/shared/{share_id}")
async def get_shared_session(share_id: str) -> dict[str, Any]:
    if not _use_in_memory_store():
        share = await _get_repository().get_share(share_id)
    else:
        share = _shares.get(share_id)
    if share is None:
        raise HTTPException(status_code=404, detail="Share not found")
    return _shared_response(share)


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
    response = await message_service.create_message(
        session_id,
        request,
        use_in_memory_store=_use_in_memory_store,
        get_repository=_get_repository,
        get_session=_get_session,
        active_run_response=_active_run_response,
        message_response=_message_response,
        run_created_response=_run_created_response,
        now_iso=_now_iso,
    )
    await _maybe_generate_session_title(session_id, request.content)
    return response


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
    model_choice = resolve_model_choice(session["request"].get("model_tier"))
    service = _build_runtime_service(_accepted_summary_memory(raw_input), model_choice=model_choice)
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
        user_id = _user_id_from_request(request)
        input_text = session["request"]["raw_input"]
        yield _sse_event("agent_event", _preparation_event(
            session_id,
            run_id,
            "stream_connected",
            "事件流已建立",
            "后端已接管运行任务，接下来会召回记忆并准备运行时上下文。",
        ))
        recalled_memories = await _recall_preference_memories(user_id, input_text)
        yield _sse_event("agent_event", _preparation_event(
            session_id,
            run_id,
            "memory_recall",
            "偏好记忆已检查",
            f"已完成长期偏好召回，命中 {len(recalled_memories)} 条。",
            {"count": len(recalled_memories)},
        ))
        if recalled_memories:
            yield _sse_event(
                "agent_event",
                {
                    "id": f"evt_mem_recalled_{uuid4().hex}",
                    "session_id": session_id,
                    "run_id": run_id,
                    "seq": 0,
                    "type": "memory_recalled",
                    "level": "info",
                    "title": "召回偏好记忆",
                    "payload": preference_memory_service.recalled_event_payload(recalled_memories),
                    "created_at": _now_iso(),
                },
            )
        summary_memory = _accepted_summary_memory(input_text)
        model_choice = resolve_model_choice(session["request"].get("model_tier"))
        yield _sse_event("agent_event", _model_selected_event(session_id, run_id, model_choice))
        service = _build_runtime_service(summary_memory, recalled_memories, model_choice)
        yield _sse_event("agent_event", _preparation_event(
            session_id,
            run_id,
            "runtime_ready",
            "运行时已准备",
            f"已装载工具注册表和摘要记忆，摘要记忆 {len(summary_memory)} 条。",
            {"summary_memory_count": len(summary_memory)},
        ))
        events = []
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=input_text,
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
        memory_result = await _store_preference_memories(user_id, session_id, run_id, input_text)
        for memory in memory_result["accepted"]:
            yield _sse_event(
                "agent_event",
                {
                    "id": f"evt_mem_accepted_{uuid4().hex}",
                    "session_id": session_id,
                    "run_id": run_id,
                    "seq": len(events) + 1,
                    "type": "memory_accepted",
                    "level": "info",
                    "title": "已保存偏好记忆",
                    "payload": preference_memory_service.memory_event_payload(memory, auto_accepted=True),
                    "created_at": _now_iso(),
                },
            )
        for candidate in memory_result["pending"]:
            yield _sse_event(
                "agent_event",
                {
                    "id": f"evt_mem_candidate_{uuid4().hex}",
                    "session_id": session_id,
                    "run_id": run_id,
                    "seq": len(events) + 1,
                    "type": "memory_candidate_created",
                    "level": "warn",
                    "title": "偏好需要确认",
                    "payload": preference_memory_service.candidate_event_payload(candidate),
                    "created_at": _now_iso(),
                },
            )
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
        user_id = _user_id_from_request(request)
        yield _sse_event("agent_event", _preparation_event(
            session_id,
            run_id,
            "stream_connected",
            "事件流已建立",
            "后端已接管运行任务，接下来会召回记忆并准备运行时上下文。",
        ))
        recalled_memories = await _recall_preference_memories(user_id, run["input_text"])
        yield _sse_event("agent_event", _preparation_event(
            session_id,
            run_id,
            "memory_recall",
            "偏好记忆已检查",
            f"已完成长期偏好召回，命中 {len(recalled_memories)} 条。",
            {"count": len(recalled_memories)},
        ))
        if recalled_memories:
            yield _sse_event(
                "agent_event",
                {
                    "id": f"evt_mem_recalled_{uuid4().hex}",
                    "session_id": session_id,
                    "run_id": run_id,
                    "seq": 0,
                    "type": "memory_recalled",
                    "level": "info",
                    "title": "召回偏好记忆",
                    "payload": preference_memory_service.recalled_event_payload(recalled_memories),
                    "created_at": _now_iso(),
                },
            )
        summary_memory = _accepted_summary_memory(run["input_text"])
        model_choice = _model_choice_from_run(run)
        yield _sse_event("agent_event", _model_selected_event(session_id, run_id, model_choice))
        service = _build_runtime_service(summary_memory, recalled_memories, model_choice)
        yield _sse_event("agent_event", _preparation_event(
            session_id,
            run_id,
            "runtime_ready",
            "运行时已准备",
            f"已装载工具注册表和摘要记忆，摘要记忆 {len(summary_memory)} 条。",
            {"summary_memory_count": len(summary_memory)},
        ))
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
        export_events = await _create_export_events_if_requested(session_id, run_id, events, len(events) + 1)
        for export_event in export_events:
            payload = export_event.model_dump(mode="json")
            events.append(payload)
            run["events"] = events
            session["events"] = events
            yield _sse_event("agent_event", payload)
        memory_result = await _store_preference_memories(user_id, session_id, run_id, run["input_text"])
        if _has_preference_memory_result(memory_result):
            _remove_legacy_memory_candidates_for_run(run_id)
        for memory in memory_result["accepted"]:
            yield _sse_event(
                "agent_event",
                {
                    "id": f"evt_mem_accepted_{uuid4().hex}",
                    "session_id": session_id,
                    "run_id": run_id,
                    "seq": len(events) + 1,
                    "type": "memory_accepted",
                    "level": "info",
                    "title": "已保存偏好记忆",
                    "payload": preference_memory_service.memory_event_payload(memory, auto_accepted=True),
                    "created_at": _now_iso(),
                },
            )
        for candidate in memory_result["pending"]:
            yield _sse_event(
                "agent_event",
                {
                    "id": f"evt_mem_candidate_{uuid4().hex}",
                    "session_id": session_id,
                    "run_id": run_id,
                    "seq": len(events) + 1,
                    "type": "memory_candidate_created",
                    "level": "warn",
                    "title": "偏好需要确认",
                    "payload": preference_memory_service.candidate_event_payload(candidate),
                    "created_at": _now_iso(),
                },
            )
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


@router.get("/memories")
async def list_memories(request: Request, status: str = Query("active")) -> dict[str, Any]:
    user_id = _user_id_from_request(request)
    if status not in {"active", "pending"}:
        raise HTTPException(status_code=400, detail="status must be active or pending")
    return {"items": await _list_preference_memories(user_id, status)}


@router.post("/memory-candidates/{candidate_id}/accept")
async def accept_memory_candidate(candidate_id: str, request: Request) -> dict[str, Any]:
    user_id = _user_id_from_request(request)
    if not _use_in_memory_store():
        memory = await preference_memory_service.accept_candidate(
            _get_repository(),
            user_id=user_id,
            candidate_id=candidate_id,
        )
        if memory is None:
            raise HTTPException(status_code=404, detail="Memory candidate not found")
        return {"memory": _memory_response(memory)}

    candidate = _memory_candidates.get(candidate_id)
    if candidate is None or candidate.get("user_id") != user_id or candidate.get("status") != "pending":
        raise HTTPException(status_code=404, detail="Memory candidate not found")
    candidate["status"] = "accepted"
    memory = {
        "id": f"mem_{uuid4().hex}",
        "user_id": user_id,
        "memory_type": candidate["memory_type"],
        "preference_key": candidate["preference_key"],
        "content": candidate["content"],
        "status": "active",
        "risk_level": candidate["risk_level"],
        "created_at": _now_iso(),
        "updated_at": _now_iso(),
    }
    _append_active_user_memory(user_id, memory)
    return {"memory": _memory_response(memory)}


@router.post("/memory-candidates/{candidate_id}/reject")
async def reject_memory_candidate(candidate_id: str, request: Request) -> dict[str, Any]:
    user_id = _user_id_from_request(request)
    if not _use_in_memory_store():
        candidate = await preference_memory_service.reject_candidate(
            _get_repository(),
            user_id=user_id,
            candidate_id=candidate_id,
        )
        if candidate is None:
            raise HTTPException(status_code=404, detail="Memory candidate not found")
        return {"candidate": _memory_candidate_response(candidate)}

    candidate = _memory_candidates.get(candidate_id)
    if candidate is None or candidate.get("user_id") != user_id:
        raise HTTPException(status_code=404, detail="Memory candidate not found")
    candidate["status"] = "rejected"
    return {"candidate": _memory_candidate_response(candidate)}


@router.delete("/memories/{memory_id}")
async def delete_memory(memory_id: str, request: Request) -> dict[str, Any]:
    user_id = _user_id_from_request(request)
    if not _use_in_memory_store():
        memory = await preference_memory_service.delete_memory(
            _get_repository(),
            user_id=user_id,
            memory_id=memory_id,
        )
        if memory is None:
            raise HTTPException(status_code=404, detail="Memory not found")
        return {"memory": _memory_response(memory)}

    for memory in _user_memories.get(user_id, []):
        if memory.get("id") == memory_id:
            memory["status"] = "deleted"
            memory["updated_at"] = _now_iso()
            return {"memory": _memory_response(memory)}
    raise HTTPException(status_code=404, detail="Memory not found")


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


def _user_id_from_request(request: Request | None) -> str:
    if request is None:
        return "system"
    value = request.headers.get("X-Agent-User-Id")
    return value.strip() if value and value.strip() else "system"


async def _list_preference_memories(user_id: str, status: str) -> list[dict[str, Any]]:
    if not _use_in_memory_store():
        return [
            _memory_candidate_response(item) if status == "pending" else _memory_response(item)
            for item in await preference_memory_service.list_memories(
                _get_repository(),
                user_id=user_id,
                status=status,
            )
        ]
    if status == "pending":
        return [
            _memory_candidate_response(candidate)
            for candidate in _memory_candidates.values()
            if candidate.get("user_id") == user_id and candidate.get("status") == "pending"
        ]
    return [
        _memory_response(memory)
        for memory in _user_memories.get(user_id, [])
        if memory.get("status") == "active"
    ]


async def _recall_preference_memories(user_id: str, query: str) -> list[dict[str, Any]]:
    if not _use_in_memory_store():
        return await preference_memory_service.recall_memories(
            _get_repository(),
            user_id=user_id,
            query=query,
        )
    memories = [
        _memory_response(memory)
        for memory in _user_memories.get(user_id, [])
        if memory.get("status") == "active"
    ]
    return preference_memory_service.rank_recalled_memories(memories, query)


async def _store_preference_memories(
    user_id: str,
    session_id: str,
    run_id: str,
    content: str,
) -> dict[str, list[dict[str, Any]]]:
    if not _use_in_memory_store():
        return await preference_memory_service.extract_and_store_preferences(
            repository=_get_repository(),
            user_id=user_id,
            session_id=session_id,
            run_id=run_id,
            content=content,
        )

    accepted: list[dict[str, Any]] = []
    pending: list[dict[str, Any]] = []
    candidates = preference_memory_service.extract_preference_candidates(
        user_id=user_id,
        session_id=session_id,
        run_id=run_id,
        content=content,
    )
    for candidate in candidates:
        status = "accepted" if candidate.proposed_action == "auto_accept" else "pending"
        candidate_record = {
            "id": candidate.id,
            "user_id": candidate.user_id,
            "session_id": candidate.session_id,
            "run_id": candidate.run_id,
            "memory_type": candidate.memory_type,
            "preference_key": candidate.preference_key,
            "content": candidate.content,
            "risk_level": candidate.risk_level,
            "reason": candidate.reason,
            "status": status,
            "created_at": _now_iso(),
        }
        _memory_candidates[candidate.id] = candidate_record
        if status == "accepted":
            memory = {
                "id": f"mem_{uuid4().hex}",
                "user_id": user_id,
                "memory_type": candidate.memory_type,
                "preference_key": candidate.preference_key,
                "content": candidate.content,
                "status": "active",
                "risk_level": candidate.risk_level,
                "created_at": _now_iso(),
                "updated_at": _now_iso(),
            }
            _append_active_user_memory(user_id, memory)
            accepted.append(memory)
        else:
            pending.append(candidate_record)
    return {"accepted": accepted, "pending": pending}


def _append_active_user_memory(user_id: str, memory: dict[str, Any]) -> None:
    memories = _user_memories.setdefault(user_id, [])
    preference_key = memory.get("preference_key") or memory.get("preferenceKey")
    if preference_key:
        for existing in memories:
            if existing.get("preference_key") == preference_key and existing.get("status") == "active":
                existing["status"] = "expired"
                existing["updated_at"] = _now_iso()
    memories.append(memory)


def _has_preference_memory_result(memory_result: dict[str, list[dict[str, Any]]]) -> bool:
    return bool(memory_result["accepted"] or memory_result["pending"])


def _remove_legacy_memory_candidates_for_run(run_id: str) -> None:
    for memory_id, memory in list(_memory_candidates.items()):
        if memory.get("run_id") == run_id and not memory.get("user_id"):
            del _memory_candidates[memory_id]


def _memory_response(memory: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": memory.get("id"),
        "memoryType": memory.get("memoryType") or memory.get("memory_type"),
        "preferenceKey": memory.get("preferenceKey") or memory.get("preference_key"),
        "content": memory.get("content"),
        "status": memory.get("status", "active"),
        "riskLevel": memory.get("riskLevel") or memory.get("risk_level"),
        "createdAt": memory.get("createdAt") or memory.get("created_at"),
        "updatedAt": memory.get("updatedAt") or memory.get("updated_at"),
    }


def _memory_candidate_response(candidate: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": candidate.get("id"),
        "memoryType": candidate.get("memoryType") or candidate.get("memory_type"),
        "preferenceKey": candidate.get("preferenceKey") or candidate.get("preference_key"),
        "content": candidate.get("content"),
        "status": candidate.get("status", "pending"),
        "riskLevel": candidate.get("riskLevel") or candidate.get("risk_level"),
        "reason": candidate.get("reason"),
        "createdAt": candidate.get("createdAt") or candidate.get("created_at"),
    }


def _build_runtime_service(
    summary_memory: list[str] | None = None,
    preference_memory: list[dict[str, Any]] | None = None,
    model_choice: ModelChoice | None = None,
) -> AgentRuntimeService:
    return runtime_factory.build_runtime_service(
        summary_memory,
        preference_memory,
        llm_model=model_choice.model if model_choice else None,
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


async def _create_run_with_model(
    repository: Any,
    session_id: str,
    run_id: str,
    model_choice: ModelChoice,
    **kwargs: Any,
) -> None:
    try:
        await repository.create_run(
            session_id,
            run_id,
            model_provider=model_choice.provider,
            model_name=model_choice.model,
            model_tier=model_choice.tier,
            **kwargs,
        )
    except TypeError:
        await repository.create_run(session_id, run_id, **kwargs)


async def _maybe_generate_session_title(session_id: str, input_text: str) -> None:
    try:
        title = await title_service.generate_session_title(input_text)
        if _use_in_memory_store():
            session = _sessions.get(session_id)
            if session is not None:
                session["title"] = title
                session["updated_at"] = _now_iso()
            return
        await _get_repository().update_session(session_id, title=title)
    except Exception:
        logger.exception("failed to generate session title session_id=%s", session_id)


async def _run_persistent_session(session_id: str) -> dict[str, Any]:
    repository = _get_repository()
    session = await repository.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    if session.get("run_id"):
        raise HTTPException(status_code=409, detail="Session already has a run")

    run_id = f"run_{uuid4().hex}"
    model_choice = resolve_model_choice()
    await _create_run_with_model(repository, session_id, run_id, model_choice)
    await _start_run_if_supported(repository, session_id, run_id)
    logger.info("agent run started session_id=%s run_id=%s mode=persistent", session_id, run_id)
    service = _build_runtime_service(
        await _list_accepted_memories(repository, session.get("raw_input") or ""),
        model_choice=model_choice,
    )
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
    user_id = _user_id_from_request(request)
    input_text = run.get("input_text") or ""
    yield _sse_event("agent_event", _preparation_event(
        session_id,
        run_id,
        "stream_connected",
        "事件流已建立",
        "后端已接管运行任务，正在标记运行状态并准备上下文。",
    ))
    await _start_run_if_supported(repository, session_id, run_id)
    recalled_memories = await _recall_preference_memories(user_id, input_text)
    yield _sse_event("agent_event", _preparation_event(
        session_id,
        run_id,
        "memory_recall",
        "偏好记忆已检查",
        f"已完成长期偏好召回，命中 {len(recalled_memories)} 条。",
        {"count": len(recalled_memories)},
    ))
    if recalled_memories:
        yield _sse_event(
            "agent_event",
            {
                "id": f"evt_mem_recalled_{uuid4().hex}",
                "session_id": session_id,
                "run_id": run_id,
                "seq": 0,
                "type": "memory_recalled",
                "level": "info",
                "title": "召回偏好记忆",
                "payload": preference_memory_service.recalled_event_payload(recalled_memories),
                "created_at": _now_iso(),
            },
        )
    summary_memory = await _list_accepted_memories(repository, input_text)
    model_choice = _model_choice_from_run(run)
    yield _sse_event("agent_event", _model_selected_event(session_id, run_id, model_choice))
    service = _build_runtime_service(
        summary_memory,
        recalled_memories,
        model_choice,
    )
    yield _sse_event("agent_event", _preparation_event(
        session_id,
        run_id,
        "runtime_ready",
        "运行时已准备",
        f"已装载工具注册表、会话上下文和摘要记忆，摘要记忆 {len(summary_memory)} 条。",
        {"summary_memory_count": len(summary_memory)},
    ))
    events: list[dict[str, Any]] = [_model_selected_event(session_id, run_id, model_choice)]
    cancelled_by_disconnect = False
    cancelled_by_request = False
    cleanup_scheduled = False
    try:
        context = await repository.list_recent_context(session_id)
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=input_text,
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
    export_events = await _create_export_events_if_requested(session_id, run_id, events, len(events) + 1)
    for export_event in export_events:
        await repository.append_event(export_event)
        payload = export_event.model_dump(mode="json")
        events.append(payload)
        await _get_redis_state().remember_last_seq(session_id, export_event.seq)
        yield _sse_event("agent_event", payload)
    memory_result = await _store_preference_memories(user_id, session_id, run_id, input_text)
    for memory in memory_result["accepted"]:
        yield _sse_event(
            "agent_event",
            {
                "id": f"evt_mem_accepted_{uuid4().hex}",
                "session_id": session_id,
                "run_id": run_id,
                "seq": len(events) + 1,
                "type": "memory_accepted",
                "level": "info",
                "title": "已保存偏好记忆",
                "payload": preference_memory_service.memory_event_payload(memory, auto_accepted=True),
                "created_at": _now_iso(),
            },
        )
    for candidate in memory_result["pending"]:
        yield _sse_event(
            "agent_event",
            {
                "id": f"evt_mem_candidate_{uuid4().hex}",
                "session_id": session_id,
                "run_id": run_id,
                "seq": len(events) + 1,
                "type": "memory_candidate_created",
                "level": "warn",
                "title": "偏好需要确认",
                "payload": preference_memory_service.candidate_event_payload(candidate),
                "created_at": _now_iso(),
            },
        )
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
        if run is None or run.get("status") != "created":
            raise HTTPException(status_code=409, detail="Session already has a run")
        run_id = session["run_id"]
    else:
        run_id = f"run_{uuid4().hex}"
        model_choice = resolve_model_choice()
        await _create_run_with_model(repository, session_id, run_id, model_choice)
        run = await repository.get_run(session_id, run_id)

    logger.info("agent stream run started session_id=%s run_id=%s mode=persistent", session_id, run_id)
    user_id = _user_id_from_request(request)
    input_text = session.get("raw_input") or ""
    yield _sse_event("agent_event", _preparation_event(
        session_id,
        run_id,
        "stream_connected",
        "事件流已建立",
        "后端已接管运行任务，正在标记运行状态并准备上下文。",
    ))
    await _start_run_if_supported(repository, session_id, run_id)
    recalled_memories = await _recall_preference_memories(user_id, input_text)
    yield _sse_event("agent_event", _preparation_event(
        session_id,
        run_id,
        "memory_recall",
        "偏好记忆已检查",
        f"已完成长期偏好召回，命中 {len(recalled_memories)} 条。",
        {"count": len(recalled_memories)},
    ))
    if recalled_memories:
        yield _sse_event(
            "agent_event",
            {
                "id": f"evt_mem_recalled_{uuid4().hex}",
                "session_id": session_id,
                "run_id": run_id,
                "seq": 0,
                "type": "memory_recalled",
                "level": "info",
                "title": "召回偏好记忆",
                "payload": preference_memory_service.recalled_event_payload(recalled_memories),
                "created_at": _now_iso(),
            },
        )
    summary_memory = await _list_accepted_memories(repository, input_text)
    model_choice = _model_choice_from_run(run)
    yield _sse_event("agent_event", _model_selected_event(session_id, run_id, model_choice))
    service = _build_runtime_service(
        summary_memory,
        recalled_memories,
        model_choice,
    )
    yield _sse_event("agent_event", _preparation_event(
        session_id,
        run_id,
        "runtime_ready",
        "运行时已准备",
        f"已装载工具注册表和摘要记忆，摘要记忆 {len(summary_memory)} 条。",
        {"summary_memory_count": len(summary_memory)},
    ))
    events: list[dict[str, Any]] = [_model_selected_event(session_id, run_id, model_choice)]
    cancelled_by_disconnect = False
    cancelled_by_request = False
    cleanup_scheduled = False
    try:
        async for event in service.run_analysis(
            session_id=session_id,
            run_id=run_id,
            raw_input=input_text,
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
    summary = _build_run_summary(events)
    await repository.create_run_summary(
        summary_id=f"sum_{uuid4().hex}",
        session_id=session_id,
        run_id=run_id,
        summary=summary,
    )
    await repository.create_message(
        message_id=f"msg_{uuid4().hex}",
        session_id=session_id,
        role="assistant",
        content=summary.get("final_answer") or summary.get("judgment") or f"本轮研判{final_status}",
        triggered_run_id=run_id,
    )
    export_events = await _create_export_events_if_requested(session_id, run_id, events, len(events) + 1)
    for export_event in export_events:
        await repository.append_event(export_event)
        payload = export_event.model_dump(mode="json")
        events.append(payload)
        await _get_redis_state().remember_last_seq(session_id, export_event.seq)
        yield _sse_event("agent_event", payload)
    memory_result = await _store_preference_memories(user_id, session_id, run_id, input_text)
    for memory in memory_result["accepted"]:
        yield _sse_event(
            "agent_event",
            {
                "id": f"evt_mem_accepted_{uuid4().hex}",
                "session_id": session_id,
                "run_id": run_id,
                "seq": len(events) + 1,
                "type": "memory_accepted",
                "level": "info",
                "title": "已保存偏好记忆",
                "payload": preference_memory_service.memory_event_payload(memory, auto_accepted=True),
                "created_at": _now_iso(),
            },
        )
    for candidate in memory_result["pending"]:
        yield _sse_event(
            "agent_event",
            {
                "id": f"evt_mem_candidate_{uuid4().hex}",
                "session_id": session_id,
                "run_id": run_id,
                "seq": len(events) + 1,
                "type": "memory_candidate_created",
                "level": "warn",
                "title": "偏好需要确认",
                "payload": preference_memory_service.candidate_event_payload(candidate),
                "created_at": _now_iso(),
            },
        )
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


def _session_response(session: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": session.get("id") or session.get("session_id"),
        "title": session.get("title") or "新研判会话",
        "status": session.get("status") or "created",
        "summary": session.get("summary"),
        "sourceType": session.get("source_type"),
        "sourceId": session.get("source_id"),
        "objectType": session.get("object_type"),
        "objectId": session.get("object_id"),
        "objectName": session.get("object_name"),
        "pinned": bool(session.get("pinned")),
        "archivedAt": session.get("archived_at"),
        "createdAt": session.get("created_at"),
        "updatedAt": session.get("updated_at"),
    }


def _in_memory_session_response(session: dict[str, Any]) -> dict[str, Any]:
    request = session.get("request", {})
    return {
        "id": session["id"],
        "title": session.get("title") or "新研判会话",
        "status": session.get("status") or "created",
        "summary": session.get("summary"),
        "source_type": request.get("source_type") or "manual",
        "source_id": request.get("source_id"),
        "object_type": request.get("object_type"),
        "object_id": request.get("object_id"),
        "object_name": request.get("object_name"),
        "pinned": bool(session.get("pinned")),
        "archived_at": session.get("archived_at"),
        "created_at": session.get("created_at"),
        "updated_at": session.get("updated_at"),
    }


def _run_created_response(session_id: str, run_id: str) -> dict[str, Any]:
    return {
        "id": run_id,
        "sessionId": session_id,
        "status": "created",
        "streamUrl": f"/manager/virtual-expert/agent/sessions/{session_id}/runs/{run_id}/stream",
    }


def _share_response(share: dict[str, Any]) -> dict[str, Any]:
    share_id = share["id"]
    share_type = share.get("share_type") or "link"
    return {
        "shareId": share_id,
        "sessionId": share.get("session_id"),
        "type": share_type,
        "title": share.get("title"),
        "shareUrl": f"/virtual-expert/share/{share_id}",
        "markdown": _share_markdown(share.get("snapshot", {})) if share_type == "md" else None,
        "createdAt": share.get("created_at"),
        "expiresAt": share.get("expires_at"),
    }


def _shared_response(share: dict[str, Any]) -> dict[str, Any]:
    return {
        "shareId": share["id"],
        "type": share.get("share_type"),
        "title": share.get("title"),
        "snapshot": share.get("snapshot", {}),
        "markdown": _share_markdown(share.get("snapshot", {})) if share.get("share_type") == "md" else None,
        "createdAt": share.get("created_at"),
    }


async def _persistent_share_snapshot(repository: AgentRepository, session_id: str) -> dict[str, Any]:
    session = await repository.get_session(session_id)
    timeline = await repository.list_timeline(session_id, limit=20)
    events = await repository.list_events(session_id, after_seq=0)
    return _build_share_snapshot(session or {"session_id": session_id}, timeline.get("items", []), events)


def _in_memory_share_snapshot(session: dict[str, Any]) -> dict[str, Any]:
    timeline = _list_in_memory_timeline(session, None, 20)
    return _build_share_snapshot(
        _in_memory_session_response(session),
        timeline.get("items", []),
        session.get("events", []),
    )


def _build_share_snapshot(
    session: dict[str, Any],
    timeline_items: list[dict[str, Any]],
    events: list[dict[str, Any]],
) -> dict[str, Any]:
    safe_events = [
        _share_event(event)
        for event in events
        if event.get("type") not in {"memory_recalled", "memory_accepted", "memory_candidate_created"}
    ]
    return {
        "session": {
            "id": session.get("id") or session.get("session_id"),
            "title": session.get("title") or "虚拟专家研判",
            "summary": session.get("summary"),
            "status": session.get("status"),
        },
        "timeline": timeline_items,
        "reactTimeline": safe_events,
        "finalAnswer": _final_answer_from_events(safe_events),
        "createdAt": _now_iso(),
    }


def _share_event(event: dict[str, Any]) -> dict[str, Any]:
    payload = event.get("payload") if isinstance(event.get("payload"), dict) else {}
    allowed_payload = {
        key: payload.get(key)
        for key in [
            "step",
            "action",
            "actions",
            "tool_name",
            "toolName",
            "summary",
            "facts",
            "documents",
            "delta",
            "content",
            "risk_level",
            "recommended_actions",
            "final_answer",
        ]
        if key in payload
    }
    return {
        "id": event.get("id"),
        "seq": event.get("seq"),
        "type": event.get("type"),
        "title": event.get("title"),
        "message": event.get("message"),
        "payload": allowed_payload,
        "createdAt": event.get("created_at") or event.get("createdAt"),
    }


def _final_answer_from_events(events: list[dict[str, Any]]) -> str:
    streamed = "".join(
        str(event.get("payload", {}).get("delta", ""))
        for event in events
        if event.get("type") == "final_answer_delta"
    ).strip()
    if streamed:
        return streamed
    recommendation = next(
        (event.get("payload", {}) for event in reversed(events) if event.get("type") == "recommendation_generated"),
        {},
    )
    return str(recommendation.get("final_answer") or recommendation.get("judgment") or "")


def _share_markdown(snapshot: dict[str, Any]) -> str:
    session = snapshot.get("session", {})
    lines = [f"# {session.get('title') or '虚拟专家研判'}", ""]
    if session.get("summary"):
        lines.extend(["## 会话摘要", str(session["summary"]), ""])
    for item in snapshot.get("timeline", []):
        user_message = item.get("user_message") or item.get("userMessage") or {}
        content = user_message.get("content")
        if content:
            lines.extend(["## 用户问题", str(content), ""])
    lines.extend(["## ReAct 过程线", ""])
    for event in snapshot.get("reactTimeline", []):
        event_type = event.get("type")
        payload = event.get("payload") or {}
        if event_type in {"llm_thinking_completed", "action_selected", "tool_completed", "knowledge_search_completed", "recommendation_generated"}:
            title = event.get("title") or event_type
            summary = payload.get("summary") or payload.get("content") or payload.get("tool_name") or payload.get("toolName")
            lines.append(f"- {title}: {summary or event_type}")
    final_answer = snapshot.get("finalAnswer")
    if final_answer:
        lines.extend(["", "## 最终答案", str(final_answer), ""])
    return "\n".join(lines).strip() + "\n"


async def _create_export_events_if_requested(
    session_id: str,
    run_id: str,
    events: list[dict[str, Any]],
    start_seq: int,
) -> list[AgentEvent]:
    export_plan = _extract_export_plan(events)
    if export_plan is None:
        return []
    try:
        export_file = await _create_export_file(session_id, run_id, export_plan)
    except Exception as exc:
        logger.exception("agent export generation failed session_id=%s run_id=%s", session_id, run_id)
        return [
            AgentEvent(
                id=f"evt_export_failed_{uuid4().hex}",
                session_id=session_id,
                run_id=run_id,
                seq=start_seq,
                type=AgentEventType.EXPORT_FAILED,
                level="error",
                title="导出文件生成失败",
                payload={
                    "error": str(exc),
                    "export_plan": export_plan,
                },
            )
        ]
    return [
        AgentEvent(
            id=f"evt_export_created_{uuid4().hex}",
            session_id=session_id,
            run_id=run_id,
            seq=start_seq,
            type=AgentEventType.EXPORT_CREATED,
            title="导出文件已生成",
            payload={
                **export_file,
                "export_plan": export_plan,
            },
        )
    ]


def _extract_export_plan(events: list[dict[str, Any]]) -> dict[str, Any] | None:
    for event in reversed(events):
        payload = event.get("payload")
        if not isinstance(payload, dict) or payload.get("tool_name") != "create_export_report":
            continue
        context = payload.get("context")
        raw = payload.get("raw")
        for candidate in (
            context.get("export_plan") if isinstance(context, dict) else None,
            raw.get("export_plan") if isinstance(raw, dict) else None,
        ):
            if isinstance(candidate, dict):
                return candidate
    return None


async def _create_export_file(session_id: str, run_id: str, export_plan: dict[str, Any]) -> dict[str, Any]:
    client = build_tool_http_client()
    raw = await client.post(
        "/internal/virtual-expert/tools/create-export",
        {
            "session_id": session_id,
            "run_id": run_id,
            "format": export_plan.get("format"),
            "export_plan": export_plan,
        },
    )
    context = raw.get("context") if isinstance(raw.get("context"), dict) else {}
    return {
        **context,
        "summary": raw.get("summary") or "导出文件已生成",
        "metadata": raw.get("metadata") or {},
    }


def _accepted_summary_memory(query: str | None, limit: int = 5) -> list[str]:
    normalized_query = (query or "").lower()
    scored: list[tuple[int, str]] = []
    for memory in _memory_candidates.values():
        if memory.get("status") != "accepted":
            continue
        if memory.get("preference_key"):
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


def _preparation_event(
    session_id: str,
    run_id: str,
    step: str,
    label: str,
    detail: str,
    extra_payload: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "id": f"evt_prep_{step}_{uuid4().hex}",
        "session_id": session_id,
        "run_id": run_id,
        "seq": 0,
        "type": AgentEventType.RUN_PREPARATION_COMPLETED,
        "level": "info",
        "title": label,
        "payload": {
            "step": step,
            "label": label,
            "detail": detail,
            **(extra_payload or {}),
        },
        "created_at": _now_iso(),
    }


def _model_selected_event(session_id: str, run_id: str, model_choice: ModelChoice) -> dict[str, Any]:
    return {
        "id": f"evt_model_selected_{uuid4().hex}",
        "session_id": session_id,
        "run_id": run_id,
        "seq": 0,
        "type": AgentEventType.MODEL_SELECTED,
        "level": "info",
        "title": "本轮模型已锁定",
        "payload": model_choice.model_dump(mode="json"),
        "created_at": _now_iso(),
    }


def _model_choice_from_run(run: dict[str, Any] | None, fallback_tier: str | None = None) -> ModelChoice:
    if run and run.get("model_name"):
        resolved = resolve_model_choice(run.get("model_tier") or fallback_tier)
        return ModelChoice(
            tier=run.get("model_tier") or resolved.tier,
            label=resolved.label,
            provider=run.get("model_provider") or resolved.provider,
            model=run["model_name"],
        )
    return resolve_model_choice(fallback_tier)


def _log_agent_event(payload: dict[str, Any]) -> None:
    stream_service.log_agent_event(logger, payload)
