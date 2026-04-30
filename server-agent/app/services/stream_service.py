import json
import logging
from collections.abc import AsyncIterator, Iterable
from typing import Any

from app.services.run_service import is_terminal_status


def sse_event(event_name: str, payload: dict[str, Any]) -> str:
    data = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    return f"event: {event_name}\ndata: {data}\n\n"


async def replay_events(
    events: Iterable[dict[str, Any]],
    *,
    session_id: str,
    run_id: str,
    status: str,
    after_seq: int = 0,
) -> AsyncIterator[str]:
    for event in events:
        if int(event.get("seq") or 0) > after_seq:
            yield sse_event("agent_event", event)
    yield sse_event("run_status", {"session_id": session_id, "run_id": run_id, "status": status})


async def replay_persistent_run(
    repository: Any,
    *,
    session_id: str,
    run_id: str,
    status: str,
    after_seq: int = 0,
) -> AsyncIterator[str]:
    events = await repository.list_run_events(session_id, run_id, after_seq)
    async for item in replay_events(
        events,
        session_id=session_id,
        run_id=run_id,
        status=status,
        after_seq=0,
    ):
        yield item


def can_replay_status(status: str | None) -> bool:
    return is_terminal_status(status)


def log_agent_event(logger: logging.Logger, payload: dict[str, Any]) -> None:
    event_type = payload.get("type")
    event_payload = payload.get("payload")
    logger.info(
        "agent event session_id=%s run_id=%s seq=%s type=%s payload=%s",
        payload.get("session_id"),
        payload.get("run_id"),
        payload.get("seq"),
        event_type,
        json.dumps(event_payload, ensure_ascii=False, default=str),
    )
