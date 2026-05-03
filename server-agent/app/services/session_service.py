from typing import Any
from uuid import uuid4

from fastapi import HTTPException


async def create_session(
    request: Any,
    *,
    use_in_memory_store,
    get_repository,
    sessions: dict[str, dict[str, Any]],
    now_iso,
    logger,
) -> dict[str, str]:
    session_id = f"ana_{uuid4().hex}"
    logger.info(
        "agent session create requested session_id=%s source_type=%s object_type=%s object_id=%s raw_input=%s",
        session_id,
        request.source_type,
        request.object_type,
        request.object_id,
        request.raw_input or request.title,
    )
    if not use_in_memory_store():
        return await get_repository().create_session(session_id, request.model_dump())

    created_at = now_iso()
    sessions[session_id] = {
        "status": "created",
        "id": session_id,
        "request": request.model_dump(),
        "title": request.title or (request.raw_input or "")[:200],
        "messages": [],
        "runs": {},
        "summaries": {},
        "events": [],
        "active_run_id": None,
        "created_at": created_at,
        "updated_at": created_at,
    }
    return {"session_id": session_id, "status": "created"}


async def list_sessions(
    limit: int,
    *,
    use_in_memory_store,
    get_repository,
    sessions: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    if not use_in_memory_store():
        return {"sessions": list(await get_repository().list_sessions(limit))}

    ordered = sorted(
        [session for session in sessions.values() if not session.get("archived_at")],
        key=lambda session: (
            1 if session.get("pinned") else 0,
            session.get("updated_at") or session.get("created_at") or "",
        ),
        reverse=True,
    )
    return {
        "sessions": [
            {
                "id": session["id"],
                "title": session.get("title") or "新研判会话",
                "status": session.get("status") or "created",
                "summary": session.get("summary"),
                "source_type": session.get("request", {}).get("source_type") or "manual",
                "source_id": session.get("request", {}).get("source_id"),
                "object_type": session.get("request", {}).get("object_type"),
                "object_id": session.get("request", {}).get("object_id"),
                "object_name": session.get("request", {}).get("object_name"),
                "pinned": bool(session.get("pinned")),
                "archived_at": session.get("archived_at"),
                "created_at": session.get("created_at"),
                "updated_at": session.get("updated_at"),
            }
            for session in ordered[:limit]
        ]
    }


async def delete_session(
    session_id: str,
    *,
    use_in_memory_store,
    get_repository,
    get_redis_state,
    sessions: dict[str, dict[str, Any]],
) -> dict[str, str]:
    if not use_in_memory_store():
        repository = get_repository()
        session = await repository.get_session(session_id)
        if session is None:
            raise HTTPException(status_code=404, detail="Session not found")
        run_id = session.get("run_id")
        if run_id:
            await get_redis_state().request_cancel(run_id)
        if not await repository.delete_session(session_id):
            raise HTTPException(status_code=404, detail="Session not found")
        return {"session_id": session_id, "status": "deleted"}

    if session_id not in sessions:
        raise HTTPException(status_code=404, detail="Session not found")
    session = sessions[session_id]
    run_id = session.get("active_run_id") or session.get("run_id")
    if run_id:
        await get_redis_state().request_cancel(run_id)
    sessions.pop(session_id, None)
    return {"session_id": session_id, "status": "deleted"}
