from dataclasses import dataclass
from typing import Any
from uuid import uuid4

PreferenceMemory = dict[str, Any]


@dataclass(frozen=True)
class PreferenceCandidate:
    id: str
    user_id: str
    session_id: str
    run_id: str
    memory_type: str
    preference_key: str
    title: str
    content: str
    risk_level: str
    proposed_action: str
    source_text: str
    reason: str
    confidence: float

    def as_record(self, status: str) -> dict[str, Any]:
        return {
            "memory_id": self.id,
            "user_id": self.user_id,
            "session_id": self.session_id,
            "run_id": self.run_id,
            "memory_type": self.memory_type,
            "preference_key": self.preference_key,
            "title": self.title,
            "content": self.content,
            "risk_level": self.risk_level,
            "proposed_action": self.proposed_action,
            "source_text": self.source_text,
            "reason": self.reason,
            "confidence": self.confidence,
            "status": status,
        }


MEMORY_TYPE_LABELS = {
    "preference": "输出偏好",
    "export_preference": "导出偏好",
    "analysis_preference": "分析偏好",
    "interaction_preference": "交互偏好",
}


def extract_preference_candidates(
    *,
    user_id: str,
    session_id: str,
    run_id: str,
    content: str,
) -> list[PreferenceCandidate]:
    normalized = content.strip()
    if not normalized:
        return []
    if _looks_like_one_time_fact(normalized):
        return []

    candidate = _classify_preference(user_id, session_id, run_id, normalized)
    return [] if candidate is None else [candidate]


def rank_recalled_memories(
    memories: list[PreferenceMemory],
    query: str,
    limit: int = 8,
) -> list[PreferenceMemory]:
    query_text = query.lower()

    def score(memory: PreferenceMemory) -> int:
        memory_type = str(memory.get("memoryType") or memory.get("memory_type") or "")
        value = 10
        if memory_type == "export_preference" and any(
            token in query_text for token in ("导出", "报告", "excel", "pdf")
        ):
            value += 20
        if memory_type == "analysis_preference" and any(
            token in query_text for token in ("分析", "风险", "研判", "处置")
        ):
            value += 15
        if memory_type == "interaction_preference":
            value += 5
        return value

    return sorted(memories, key=score, reverse=True)[:limit]


def format_recalled_memories_for_prompt(memories: list[PreferenceMemory]) -> str:
    if not memories:
        return ""
    lines = ["用户长期偏好："]
    for memory in memories:
        memory_type = str(memory.get("memoryType") or memory.get("memory_type") or "preference")
        label = MEMORY_TYPE_LABELS.get(memory_type, "偏好")
        lines.append(f"- {label}：{memory.get('content', '')}")
    return "\n".join(lines)


def memory_event_payload(memory: dict[str, Any], *, auto_accepted: bool) -> dict[str, Any]:
    return {
        "memoryId": memory.get("id"),
        "content": memory.get("content"),
        "memoryType": memory.get("memory_type") or memory.get("memoryType"),
        "riskLevel": memory.get("risk_level") or memory.get("riskLevel"),
        "autoAccepted": auto_accepted,
    }


def candidate_event_payload(candidate: dict[str, Any]) -> dict[str, Any]:
    return {
        "candidateId": candidate.get("id"),
        "content": candidate.get("content"),
        "memoryType": candidate.get("memory_type") or candidate.get("memoryType"),
        "riskLevel": candidate.get("risk_level") or candidate.get("riskLevel"),
        "reason": candidate.get("reason"),
    }


def recalled_event_payload(memories: list[PreferenceMemory]) -> dict[str, Any]:
    return {
        "count": len(memories),
        "items": [
            {
                "memoryId": memory.get("id"),
                "memoryType": memory.get("memoryType") or memory.get("memory_type"),
                "content": memory.get("content"),
            }
            for memory in memories
        ],
    }


async def extract_and_store_preferences(
    *,
    repository,
    user_id: str,
    session_id: str,
    run_id: str,
    content: str,
) -> dict[str, list[dict[str, Any]]]:
    accepted: list[dict[str, Any]] = []
    pending: list[dict[str, Any]] = []
    for candidate in extract_preference_candidates(
        user_id=user_id,
        session_id=session_id,
        run_id=run_id,
        content=content,
    ):
        status = "accepted" if candidate.proposed_action == "auto_accept" else "pending"
        created = await repository.create_memory_candidate_v2(**candidate.as_record(status))
        if status == "accepted":
            memory = await repository.create_user_memory(
                memory_id=f"mem_{uuid4().hex}",
                user_id=user_id,
                memory_type=candidate.memory_type,
                preference_key=candidate.preference_key,
                content=candidate.content,
                risk_level=candidate.risk_level,
                source_candidate_id=created["id"],
                source_session_id=session_id,
                source_run_id=run_id,
            )
            accepted.append(memory)
        else:
            pending.append(created)
    return {"accepted": accepted, "pending": pending}


async def accept_candidate(repository, *, user_id: str, candidate_id: str) -> dict[str, Any] | None:
    candidate = await repository.get_memory_candidate(user_id, candidate_id)
    if candidate is None or candidate.get("status") != "pending":
        return None
    memory = await repository.create_user_memory(
        memory_id=f"mem_{uuid4().hex}",
        user_id=user_id,
        memory_type=candidate["memory_type"],
        preference_key=candidate["preference_key"],
        content=candidate["content"],
        risk_level=candidate["risk_level"] or "high",
        source_candidate_id=candidate["id"],
        source_session_id=candidate["session_id"],
        source_run_id=candidate["run_id"],
    )
    await repository.set_memory_candidate_status(user_id, candidate_id, "accepted", user_id)
    return memory


async def reject_candidate(repository, *, user_id: str, candidate_id: str) -> dict[str, Any] | None:
    return await repository.set_memory_candidate_status(user_id, candidate_id, "rejected", user_id)


async def delete_memory(repository, *, user_id: str, memory_id: str) -> dict[str, Any] | None:
    return await repository.delete_user_memory(user_id, memory_id)


async def list_memories(repository, *, user_id: str, status: str) -> list[dict[str, Any]]:
    if status == "pending":
        return await repository.list_memory_candidates(user_id, "pending")
    return await repository.list_user_memories(user_id, "active")


async def recall_memories(
    repository,
    *,
    user_id: str,
    query: str,
    limit: int = 8,
) -> list[dict[str, Any]]:
    memories = await repository.list_user_memories(user_id, "active")
    normalized = [
        {
            "id": memory.get("id"),
            "memoryType": memory.get("memory_type"),
            "content": memory.get("content"),
            "riskLevel": memory.get("risk_level"),
            "preferenceKey": memory.get("preference_key"),
        }
        for memory in memories
    ]
    return rank_recalled_memories(normalized, query, limit)


def _classify_preference(
    user_id: str,
    session_id: str,
    run_id: str,
    content: str,
) -> PreferenceCandidate | None:
    if any(token in content for token in ("高等级", "高风险", "忽略低级告警", "不要展示")):
        return _candidate(
            user_id,
            session_id,
            run_id,
            content,
            memory_type="analysis_preference",
            preference_key="analysis.risk_policy",
            normalized_content=_strip_memory_prefix(content),
            risk_level="high",
            reason="该偏好会影响后续风险研判口径，需要用户确认。",
            confidence=0.86,
        )
    if "导出" in content or "报告" in content or "excel" in content.lower() or "pdf" in content.lower():
        return _candidate(
            user_id,
            session_id,
            run_id,
            content,
            memory_type="export_preference",
            preference_key="export.default_format",
            normalized_content=_strip_memory_prefix(content),
            risk_level="low",
            reason="用户表达了导出或报告格式偏好。",
            confidence=0.82,
        )
    if any(token in content for token in ("不确定", "先问", "追问")):
        return _candidate(
            user_id,
            session_id,
            run_id,
            content,
            memory_type="interaction_preference",
            preference_key="interaction.ask_when_uncertain",
            normalized_content=_strip_memory_prefix(content),
            risk_level="low",
            reason="用户表达了信息不足时的交互偏好。",
            confidence=0.82,
        )
    if any(
        token in content
        for token in ("记住", "以后", "默认", "下次", "我喜欢", "最好", "别", "不要", "先", "尽量", "短一点", "用表格")
    ):
        return _candidate(
            user_id,
            session_id,
            run_id,
            content,
            memory_type="preference",
            preference_key="answer.conclusion_first" if "结论" in content or "先" in content else "answer.style",
            normalized_content=_strip_memory_prefix(content),
            risk_level="low",
            reason="用户表达了回答方式偏好。",
            confidence=0.8,
        )
    return None


def _candidate(
    user_id: str,
    session_id: str,
    run_id: str,
    source_text: str,
    *,
    memory_type: str,
    preference_key: str,
    normalized_content: str,
    risk_level: str,
    reason: str,
    confidence: float,
) -> PreferenceCandidate:
    return PreferenceCandidate(
        id=f"cand_{uuid4().hex}",
        user_id=user_id,
        session_id=session_id,
        run_id=run_id,
        memory_type=memory_type,
        preference_key=preference_key,
        title=normalized_content[:80],
        content=normalized_content,
        risk_level=risk_level,
        proposed_action="auto_accept" if risk_level == "low" else "needs_confirmation",
        source_text=source_text,
        reason=reason,
        confidence=confidence,
    )


def _strip_memory_prefix(content: str) -> str:
    stripped = content.strip()
    for prefix in ("请记住", "记住", "以后", "默认", "下次"):
        if stripped.startswith(prefix):
            stripped = stripped.removeprefix(prefix).strip(" ：:，,")
            break
    return stripped or content


def _looks_like_one_time_fact(content: str) -> bool:
    fact_markers = ("今天", "当前", "刚刚", "本次", "这次")
    preference_markers = ("记住", "以后", "默认", "下次", "我喜欢", "最好", "别", "不要", "先", "尽量")
    return any(marker in content for marker in fact_markers) and not any(
        marker in content for marker in preference_markers
    )
