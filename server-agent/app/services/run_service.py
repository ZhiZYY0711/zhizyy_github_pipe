import re
from typing import Any
from uuid import uuid4

from app.agent.state import RunStatus

TERMINAL_RUN_STATUSES = {"completed", "failed", "cancelled", "awaiting_user"}


def is_terminal_status(status: str | None) -> bool:
    return status in TERMINAL_RUN_STATUSES


def derive_run_status(events: list[dict[str, Any]]) -> RunStatus:
    if not events:
        return "failed"

    terminal_status_by_event_type: dict[str, RunStatus] = {
        "run_completed": "completed",
        "run_failed": "failed",
        "run_cancelled": "cancelled",
        "awaiting_user": "awaiting_user",
    }
    return terminal_status_by_event_type.get(events[-1]["type"], "failed")


def build_run_summary(events: list[dict[str, Any]]) -> dict[str, Any]:
    final_answer = "".join(
        str(event.get("payload", {}).get("delta", ""))
        for event in events
        if event.get("type") == "final_answer_delta"
    ).strip()
    recommendation = next(
        (event.get("payload", {}) for event in events if event.get("type") == "recommendation_generated"),
        {},
    )
    tool_summary = [
        {
            "tool_name": event.get("payload", {}).get("tool_name") or event.get("payload", {}).get("toolName"),
            "summary": event.get("payload", {}).get("summary") or event.get("title"),
        }
        for event in events
        if event.get("type") == "tool_completed"
    ]
    knowledge_summary = [
        {
            "source": event.get("payload", {}).get("source") or event.get("type"),
            "documents": event.get("payload", {}).get("documents", []),
        }
        for event in events
        if event.get("type") in {"knowledge_search_completed", "retrieval_completed"}
    ]
    key_evidence = [
        *[item for item in tool_summary if item.get("summary")],
        *[
            {"source": item.get("source"), "summary": str(item.get("documents", []))}
            for item in knowledge_summary
        ],
    ]
    recommended_actions = recommendation.get("recommended_actions") or recommendation.get("recommendedActions") or recommendation.get("actions") or []
    open_questions = recommendation.get("missing_information") or recommendation.get("missingInformation") or []
    judgment = recommendation.get("judgment") or recommendation.get("conclusion") or recommendation.get("summary")
    return {
        "final_answer": final_answer or judgment or "",
        "risk_level": recommendation.get("risk_level") or recommendation.get("riskLevel"),
        "judgment": judgment or "",
        "recommended_actions": recommended_actions if isinstance(recommended_actions, list) else [],
        "key_evidence": key_evidence,
        "open_questions": open_questions if isinstance(open_questions, list) else [],
        "tool_summary": tool_summary,
        "knowledge_summary": knowledge_summary,
    }


def complete_in_memory_message_run(
    session: dict[str, Any],
    run: dict[str, Any],
    events: list[dict[str, Any]],
    final_status: RunStatus,
    *,
    memory_candidates: dict[str, dict[str, Any]],
    now_iso,
) -> None:
    summary = build_run_summary(events)
    now = now_iso()
    run["status"] = final_status
    run["completed_at"] = now
    run["finished_at"] = now
    run["event_count"] = len(events)
    run["last_event_seq"] = events[-1]["seq"] if events else 0
    run["failure_reason"] = None if final_status != "failed" else "runtime ended without terminal success event"
    run["summary"] = summary
    store_in_memory_candidates(session, run, summary, memory_candidates=memory_candidates, now_iso=now_iso)
    session["summaries"][run["id"]] = summary
    session["active_run_id"] = None
    session["status"] = final_status
    session["summary"] = summary.get("judgment") or summary.get("final_answer")
    session["messages"].append(
        {
            "id": f"msg_{uuid4().hex}",
            "session_id": run["session_id"],
            "role": "assistant",
            "content": summary.get("final_answer") or summary.get("judgment") or f"本轮研判{final_status}",
            "message_type": "text",
            "triggered_run_id": run["id"],
            "created_at": now,
        }
    )


def store_in_memory_candidates(
    session: dict[str, Any],
    run: dict[str, Any],
    summary: dict[str, Any],
    *,
    memory_candidates: dict[str, dict[str, Any]],
    now_iso,
) -> None:
    content = str(run.get("input_text") or "")
    if not any(word in content for word in ("记住", "以后", "默认", "下次")):
        return
    memory_type = "export_preference" if any(word in content.lower() for word in ("导出", "pdf", "excel", "报告")) else "preference"
    memory_id = f"mem_{uuid4().hex}"
    memory_candidates[memory_id] = {
        "id": memory_id,
        "session_id": session["id"],
        "run_id": run["id"],
        "memory_type": memory_type,
        "title": content[:80],
        "content": clean_memory_content(content),
        "confidence": 0.9,
        "status": "accepted",
        "created_at": now_iso(),
        "summary": summary,
    }


def clean_memory_content(content: str) -> str:
    return re.sub(r"^(请)?记住[：:，,\\s]*", "", content).strip() or content
