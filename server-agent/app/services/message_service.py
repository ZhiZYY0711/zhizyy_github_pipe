import json
from typing import Any
from uuid import uuid4

from fastapi import HTTPException


async def create_message(
    session_id: str,
    request: Any,
    *,
    use_in_memory_store,
    get_repository,
    get_session,
    infer_export_plan,
    active_run_response,
    message_response,
    run_created_response,
    now_iso,
) -> dict[str, Any]:
    if not use_in_memory_store():
        return await _create_persistent_message(
            session_id,
            request,
            get_repository=get_repository,
            infer_export_plan=infer_export_plan,
            active_run_response=active_run_response,
            message_response=message_response,
            run_created_response=run_created_response,
        )

    session = get_session(session_id)
    active_run_id = session.get("active_run_id")
    if active_run_id and session["runs"].get(active_run_id, {}).get("status") in {"created", "running"}:
        return active_run_response(session_id, active_run_id)

    message_id = f"msg_{uuid4().hex}"
    run_id = f"run_{uuid4().hex}"
    created_at = now_iso()
    message = {
        "id": message_id,
        "session_id": session_id,
        "role": "user",
        "content": request.content,
        "message_type": request.message_type,
        "created_at": created_at,
    }
    session["messages"].append(message)

    export_plan = await infer_export_plan(request.content)
    if export_plan is not None:
        session["messages"].append(
            {
                "id": f"msg_{uuid4().hex}",
                "session_id": session_id,
                "role": "assistant",
                "content": json.dumps(export_plan, ensure_ascii=False),
                "message_type": "export_plan",
                "created_at": now_iso(),
            }
        )
        session["updated_at"] = created_at
        return {
            "message": message_response(message),
            "export_plan": export_plan,
            "run": None,
        }

    session["runs"][run_id] = {
        "id": run_id,
        "session_id": session_id,
        "triggering_message_id": message_id,
        "input_text": request.content,
        "status": "created",
        "events": [],
        "created_at": created_at,
    }
    session["active_run_id"] = run_id
    session["run_id"] = run_id
    session["status"] = "running"
    session["updated_at"] = created_at
    return {
        "message": message_response(message),
        "run": run_created_response(session_id, run_id),
    }


async def _create_persistent_message(
    session_id: str,
    request: Any,
    *,
    get_repository,
    infer_export_plan,
    active_run_response,
    message_response,
    run_created_response,
) -> dict[str, Any]:
    repository = get_repository()
    session = await repository.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    active_run = await repository.find_active_run(session_id)
    if active_run is not None:
        return active_run_response(session_id, active_run["id"])

    message_id = f"msg_{uuid4().hex}"
    run_id = f"run_{uuid4().hex}"
    message = await repository.create_message(
        message_id=message_id,
        session_id=session_id,
        role="user",
        content=request.content,
        message_type=request.message_type,
    )
    export_plan = await infer_export_plan(request.content)
    if export_plan is not None:
        await repository.create_message(
            message_id=f"msg_{uuid4().hex}",
            session_id=session_id,
            role="assistant",
            content=json.dumps(export_plan, ensure_ascii=False),
            message_type="export_plan",
        )
        return {
            "message": message_response(message),
            "export_plan": export_plan,
            "run": None,
        }

    await repository.create_run(
        session_id=session_id,
        run_id=run_id,
        triggering_message_id=message_id,
        input_text=request.content,
    )
    return {
        "message": message_response(message),
        "run": run_created_response(session_id, run_id),
    }
