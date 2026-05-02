# Agent Memory v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Memory v2 phase 1 so the virtual expert visibly remembers, confirms, lists, recalls, and applies current-user preference memories.

**Architecture:** Split candidate memories from active memories by adding `user_memory`. Keep `memory_candidate` as the extraction/review queue, add a focused `PreferenceMemoryService` for extraction, acceptance, rejection, deletion, recall, and SSE event payloads, then expose the same behavior through Spring proxy endpoints and a lightweight frontend memory UI.

**Tech Stack:** FastAPI, Pydantic v2, SQLAlchemy async, PostgreSQL, pytest, Spring Boot RestTemplate/HttpClient proxy, Vue 3 + TypeScript + Vitest.

---

## File Structure

- Create `server-agent/app/services/preference_memory_service.py`
  - Owns preference extraction rules, risk classification, in-memory and persistent candidate/active memory workflows, recall ranking, and display payload formatting.
- Modify `server-agent/app/persistence/models.py`
  - Add `UserMemoryModel`; extend `MemoryCandidateModel` with user and extraction metadata.
- Modify `server-agent/app/persistence/repository.py`
  - Add repository methods for candidate creation/listing/review and active user memory create/list/delete/recall.
- Modify `server-agent/app/agent/events.py`
  - Add memory event types used by SSE and frontend.
- Modify `server-agent/app/api/sessions.py`
  - Add user-id extraction, memory API routes, and memory event emission around run execution.
- Modify `server-agent/app/agent/service.py`, `server-agent/app/react/runtime.py`, and `server-agent/app/react/prompts.py`
  - Rename injected preference memories at runtime while preserving `summary_memory` compatibility.
- Modify `database/postgres/01-init/agent_init_only_structure.sql` and `database/postgres/01-init/agent_init_structure_data.sql`
  - Add schema DDL for `user_memory` and new `memory_candidate` columns.
- Modify `server/server-web/src/main/java/com/gxa/pipe/virtualExpert/agent/AgentClient.java`
  - Add proxy methods for memory APIs and forward `X-Agent-User-Id`.
- Modify `server/server-web/src/main/java/com/gxa/pipe/virtualExpert/agent/VirtualExpertAgentController.java`
  - Add `/memories` and `/memory-candidates/{id}/...` endpoints.
- Modify `frontend/src/modules/virtual-expert/types.ts`
  - Add memory item, candidate, and response types.
- Modify `frontend/src/modules/virtual-expert/api.ts`
  - Add memory API client functions.
- Modify `frontend/src/modules/virtual-expert/service.ts`
  - Normalize memory events, add memory API orchestration helpers, and labels for memory event display.
- Modify `frontend/src/modules/virtual-expert/service.test.ts`
  - Add event-label and memory normalization tests.
- Modify `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue`
  - Add visible memory notices, pending confirmation cards, "我的偏好" list drawer, and recall hint.

## Task 1: Persistence Model And Repository

**Files:**
- Modify: `server-agent/app/persistence/models.py`
- Modify: `server-agent/app/persistence/repository.py`
- Modify: `server-agent/tests/test_persistence_models.py`
- Create: `server-agent/tests/test_preference_memory_service.py`

- [ ] **Step 1: Write model tests for candidate metadata and active memories**

Add these tests to `server-agent/tests/test_persistence_models.py`:

```python
from app.persistence.models import MemoryCandidateModel, UserMemoryModel


def test_memory_candidate_has_preference_metadata_columns() -> None:
    columns = MemoryCandidateModel.__table__.columns

    assert "user_id" in columns
    assert "candidate_type" in columns
    assert "preference_key" in columns
    assert "risk_level" in columns
    assert "proposed_action" in columns
    assert "source_text" in columns
    assert "reason" in columns


def test_user_memory_model_declares_active_memory_columns() -> None:
    assert UserMemoryModel.__tablename__ == "user_memory"
    columns = UserMemoryModel.__table__.columns

    assert "id" in columns
    assert "user_id" in columns
    assert "memory_type" in columns
    assert "preference_key" in columns
    assert "content" in columns
    assert "status" in columns
    assert "risk_level" in columns
    assert "source_candidate_id" in columns
    assert "source_session_id" in columns
    assert "source_run_id" in columns
    assert "expires_at" in columns
```

- [ ] **Step 2: Run model tests and verify they fail**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_persistence_models.py::test_memory_candidate_has_preference_metadata_columns tests/test_persistence_models.py::test_user_memory_model_declares_active_memory_columns -q
```

Expected: FAIL because `UserMemoryModel` and new `MemoryCandidateModel` columns do not exist.

- [ ] **Step 3: Add SQLAlchemy model fields**

In `server-agent/app/persistence/models.py`, update imports and models as follows:

```python
from sqlalchemy import BigInteger, CheckConstraint, DateTime, ForeignKey, Numeric, String, Text
```

Extend `MemoryCandidateModel`:

```python
    user_id: Mapped[str] = mapped_column(String(64), default="system", nullable=False)
    candidate_type: Mapped[str | None] = mapped_column(String(64))
    preference_key: Mapped[str | None] = mapped_column(String(128))
    risk_level: Mapped[str | None] = mapped_column(String(16))
    proposed_action: Mapped[str | None] = mapped_column(String(32))
    source_text: Mapped[str | None] = mapped_column(Text)
    reason: Mapped[str | None] = mapped_column(Text)
```

Add this model after `MemoryCandidateModel`:

```python
class UserMemoryModel(Base):
    __tablename__ = "user_memory"
    __table_args__ = (
        CheckConstraint(
            "status IN ('active', 'deleted', 'expired')",
            name="ck_user_memory_status",
        ),
        CheckConstraint(
            "risk_level IN ('low', 'high')",
            name="ck_user_memory_risk_level",
        ),
    )

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64), nullable=False)
    memory_type: Mapped[str] = mapped_column(String(64), nullable=False)
    preference_key: Mapped[str] = mapped_column(String(128), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[str] = mapped_column(String(32), default="active", nullable=False)
    risk_level: Mapped[str] = mapped_column(String(16), default="low", nullable=False)
    source_candidate_id: Mapped[str | None] = mapped_column(String(64))
    source_session_id: Mapped[str | None] = mapped_column(String(64))
    source_run_id: Mapped[str | None] = mapped_column(String(64))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
```

- [ ] **Step 4: Run model tests and verify they pass**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_persistence_models.py::test_memory_candidate_has_preference_metadata_columns tests/test_persistence_models.py::test_user_memory_model_declares_active_memory_columns -q
```

Expected: PASS.

- [ ] **Step 5: Write repository unit tests using a fake session factory seam**

Create `server-agent/tests/test_preference_memory_service.py` with pure service tests first; repository integration gets exercised through API tests in Task 3. Use this initial content:

```python
from app.services.preference_memory_service import (
    extract_preference_candidates,
    format_recalled_memories_for_prompt,
    rank_recalled_memories,
)


def test_extracts_explicit_low_risk_preference() -> None:
    candidates = extract_preference_candidates(
        user_id="u_1",
        session_id="ana_1",
        run_id="run_1",
        content="以后回答先给结论，别写太长",
    )

    assert len(candidates) == 1
    assert candidates[0].memory_type == "preference"
    assert candidates[0].preference_key == "answer.conclusion_first"
    assert candidates[0].risk_level == "low"
    assert candidates[0].proposed_action == "auto_accept"
    assert "先给结论" in candidates[0].content


def test_extracts_high_risk_analysis_preference_for_confirmation() -> None:
    candidates = extract_preference_candidates(
        user_id="u_1",
        session_id="ana_1",
        run_id="run_1",
        content="以后风险都按高等级处理",
    )

    assert len(candidates) == 1
    assert candidates[0].memory_type == "analysis_preference"
    assert candidates[0].risk_level == "high"
    assert candidates[0].proposed_action == "needs_confirmation"


def test_does_not_extract_one_time_business_fact() -> None:
    candidates = extract_preference_candidates(
        user_id="u_1",
        session_id="ana_1",
        run_id="run_1",
        content="CP-04 今天压力波动明显",
    )

    assert candidates == []


def test_rank_recalled_memories_prioritizes_export_when_query_mentions_report() -> None:
    memories = [
        {"memoryType": "preference", "content": "回答先给结论。"},
        {"memoryType": "export_preference", "content": "默认导出 Excel。"},
    ]

    ranked = rank_recalled_memories(memories, "帮我导出报告")

    assert ranked[0]["memoryType"] == "export_preference"


def test_format_recalled_memories_for_prompt_groups_by_type() -> None:
    text = format_recalled_memories_for_prompt(
        [
            {"memoryType": "preference", "content": "回答先给结论。"},
            {"memoryType": "interaction_preference", "content": "信息不足时先追问。"},
        ]
    )

    assert "用户长期偏好" in text
    assert "输出偏好：回答先给结论。" in text
    assert "交互偏好：信息不足时先追问。" in text
```

- [ ] **Step 6: Run service tests and verify import failure**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_preference_memory_service.py -q
```

Expected: FAIL because `app.services.preference_memory_service` does not exist.

- [ ] **Step 7: Add repository methods**

In `server-agent/app/persistence/repository.py`, import `UserMemoryModel` and add these helper serializers:

```python
def _memory_candidate_to_dict(memory: MemoryCandidateModel) -> dict[str, Any]:
    return {
        "id": memory.id,
        "user_id": memory.user_id,
        "session_id": memory.session_id,
        "run_id": memory.run_id,
        "memory_type": memory.memory_type,
        "candidate_type": memory.candidate_type or memory.memory_type,
        "preference_key": memory.preference_key,
        "title": memory.title,
        "content": memory.content,
        "risk_level": memory.risk_level,
        "proposed_action": memory.proposed_action,
        "source_text": memory.source_text,
        "reason": memory.reason,
        "status": memory.status,
        "confidence": None if memory.confidence is None else float(memory.confidence),
        "created_at": memory.created_at.isoformat() if memory.created_at is not None else None,
        "reviewed_at": memory.reviewed_at.isoformat() if memory.reviewed_at is not None else None,
        "reviewed_by": memory.reviewed_by,
    }


def _user_memory_to_dict(memory: UserMemoryModel) -> dict[str, Any]:
    return {
        "id": memory.id,
        "user_id": memory.user_id,
        "memory_type": memory.memory_type,
        "preference_key": memory.preference_key,
        "content": memory.content,
        "status": memory.status,
        "risk_level": memory.risk_level,
        "source_candidate_id": memory.source_candidate_id,
        "source_session_id": memory.source_session_id,
        "source_run_id": memory.source_run_id,
        "created_at": memory.created_at.isoformat() if memory.created_at is not None else None,
        "updated_at": memory.updated_at.isoformat() if memory.updated_at is not None else None,
        "expires_at": memory.expires_at.isoformat() if memory.expires_at is not None else None,
    }
```

Add these methods to `AgentRepository`:

```python
    async def create_memory_candidate_v2(
        self,
        *,
        memory_id: str,
        user_id: str,
        session_id: str,
        run_id: str,
        memory_type: str,
        preference_key: str,
        title: str,
        content: str,
        risk_level: str,
        proposed_action: str,
        source_text: str,
        reason: str,
        status: str,
        confidence: float | None = None,
    ) -> dict[str, Any]:
        async with self._session_factory() as db:
            model = MemoryCandidateModel(
                id=memory_id,
                user_id=user_id,
                session_id=session_id,
                run_id=run_id,
                memory_type=memory_type,
                candidate_type=memory_type,
                preference_key=preference_key,
                title=title[:200],
                content=content,
                risk_level=risk_level,
                proposed_action=proposed_action,
                source_text=source_text,
                reason=reason,
                status=status,
                confidence=confidence,
            )
            db.add(model)
            await db.commit()
            return _memory_candidate_to_dict(model)

    async def create_user_memory(
        self,
        *,
        memory_id: str,
        user_id: str,
        memory_type: str,
        preference_key: str,
        content: str,
        risk_level: str,
        source_candidate_id: str | None,
        source_session_id: str | None,
        source_run_id: str | None,
    ) -> dict[str, Any]:
        async with self._session_factory() as db:
            result = await db.execute(
                select(UserMemoryModel).where(
                    UserMemoryModel.user_id == user_id,
                    UserMemoryModel.preference_key == preference_key,
                    UserMemoryModel.status == "active",
                )
            )
            for existing in result.scalars():
                existing.status = "expired"
                existing.updated_at = func.now()
            model = UserMemoryModel(
                id=memory_id,
                user_id=user_id,
                memory_type=memory_type,
                preference_key=preference_key,
                content=content,
                risk_level=risk_level,
                source_candidate_id=source_candidate_id,
                source_session_id=source_session_id,
                source_run_id=source_run_id,
            )
            db.add(model)
            await db.commit()
            return _user_memory_to_dict(model)

    async def list_user_memories(self, user_id: str, status: str = "active") -> list[dict[str, Any]]:
        async with self._session_factory() as db:
            result = await db.execute(
                select(UserMemoryModel)
                .where(UserMemoryModel.user_id == user_id, UserMemoryModel.status == status)
                .order_by(UserMemoryModel.updated_at.desc(), UserMemoryModel.created_at.desc())
            )
            return [_user_memory_to_dict(memory) for memory in result.scalars()]

    async def list_memory_candidates(self, user_id: str, status: str = "pending") -> list[dict[str, Any]]:
        async with self._session_factory() as db:
            result = await db.execute(
                select(MemoryCandidateModel)
                .where(MemoryCandidateModel.user_id == user_id, MemoryCandidateModel.status == status)
                .order_by(MemoryCandidateModel.created_at.desc())
            )
            return [_memory_candidate_to_dict(memory) for memory in result.scalars()]

    async def get_memory_candidate(self, user_id: str, candidate_id: str) -> dict[str, Any] | None:
        async with self._session_factory() as db:
            candidate = await db.get(MemoryCandidateModel, candidate_id)
            if candidate is None or candidate.user_id != user_id:
                return None
            return _memory_candidate_to_dict(candidate)

    async def set_memory_candidate_status(
        self,
        user_id: str,
        candidate_id: str,
        status: str,
        reviewed_by: str,
    ) -> dict[str, Any] | None:
        async with self._session_factory() as db:
            candidate = await db.get(MemoryCandidateModel, candidate_id)
            if candidate is None or candidate.user_id != user_id:
                return None
            candidate.status = status
            candidate.reviewed_by = reviewed_by
            candidate.reviewed_at = func.now()
            await db.commit()
            return _memory_candidate_to_dict(candidate)

    async def delete_user_memory(self, user_id: str, memory_id: str) -> dict[str, Any] | None:
        async with self._session_factory() as db:
            memory = await db.get(UserMemoryModel, memory_id)
            if memory is None or memory.user_id != user_id:
                return None
            memory.status = "deleted"
            memory.updated_at = func.now()
            await db.commit()
            return _user_memory_to_dict(memory)
```

- [ ] **Step 8: Commit persistence model and repository changes**

Run:

```bash
git add server-agent/app/persistence/models.py server-agent/app/persistence/repository.py server-agent/tests/test_persistence_models.py server-agent/tests/test_preference_memory_service.py
git commit -m "feat(agent): add preference memory persistence"
```

Expected: commit succeeds with only persistence and initial service test files staged.

## Task 2: Preference Memory Service

**Files:**
- Create: `server-agent/app/services/preference_memory_service.py`
- Modify: `server-agent/tests/test_preference_memory_service.py`

- [ ] **Step 1: Implement extraction dataclass and pure extraction rules**

Create `server-agent/app/services/preference_memory_service.py`:

```python
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
    if "导出" in content or "报告" in content or "Excel" in content or "PDF" in content or "pdf" in content.lower():
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
    if any(token in content for token in ("记住", "以后", "默认", "下次", "我喜欢", "最好", "别", "不要", "先", "尽量", "短一点", "用表格")):
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
```

- [ ] **Step 2: Implement ranking and prompt formatting**

Append:

```python
MEMORY_TYPE_LABELS = {
    "preference": "输出偏好",
    "export_preference": "导出偏好",
    "analysis_preference": "分析偏好",
    "interaction_preference": "交互偏好",
}


def rank_recalled_memories(memories: list[PreferenceMemory], query: str, limit: int = 8) -> list[PreferenceMemory]:
    query_text = query.lower()

    def score(memory: PreferenceMemory) -> int:
        memory_type = str(memory.get("memoryType") or memory.get("memory_type") or "")
        value = 10
        if memory_type == "export_preference" and any(token in query_text for token in ("导出", "报告", "excel", "pdf")):
            value += 20
        if memory_type == "analysis_preference" and any(token in query_text for token in ("分析", "风险", "研判", "处置")):
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
```

- [ ] **Step 3: Implement async service wrappers**

Append:

```python
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


async def recall_memories(repository, *, user_id: str, query: str, limit: int = 8) -> list[dict[str, Any]]:
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
```

- [ ] **Step 4: Run preference memory service tests**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_preference_memory_service.py -q
```

Expected: PASS.

- [ ] **Step 5: Commit preference memory service**

Run:

```bash
git add server-agent/app/services/preference_memory_service.py server-agent/tests/test_preference_memory_service.py
git commit -m "feat(agent): add preference memory service"
```

Expected: commit succeeds with service and tests only.

## Task 3: Agent API, SSE Events, And Runtime Injection

**Files:**
- Modify: `server-agent/app/agent/events.py`
- Modify: `server-agent/app/api/sessions.py`
- Modify: `server-agent/app/agent/service.py`
- Modify: `server-agent/app/react/runtime.py`
- Modify: `server-agent/app/react/prompts.py`
- Modify: `server-agent/tests/test_sessions_api.py`
- Modify: `server-agent/tests/test_react_runtime.py`

- [ ] **Step 1: Add failing API tests for memory visibility**

Append to `server-agent/tests/test_sessions_api.py`:

```python
def test_low_risk_preference_emits_memory_accepted_and_is_recalled(monkeypatch):
    class MemoryService:
        def __init__(self, tool_registry, summary_memory=None, preference_memory=None) -> None:
            self.preference_memory = preference_memory or summary_memory or []

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str], conversation_context=None):
            yield AgentEvent(
                id=f"evt_{run_id}_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.CONTEXT_BUILT,
                payload={"preference_memory": self.preference_memory},
            )
            yield AgentEvent(id=f"evt_{run_id}_002", session_id=session_id, run_id=run_id, seq=2, type=AgentEventType.RUN_COMPLETED)

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", MemoryService)
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "偏好测试"}).json()
    first = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "以后回答先给结论"},
    ).json()

    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/{first['run']['id']}/stream") as response:
        body = response.read().decode("utf-8")

    assert '"type":"memory_accepted"' in body
    assert "以后回答先给结论" in body

    second = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "压力波动怎么处理"},
    ).json()
    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/{second['run']['id']}/stream") as response:
        recalled = response.read().decode("utf-8")

    assert '"type":"memory_recalled"' in recalled
    assert "以后回答先给结论" in recalled


def test_high_risk_preference_creates_pending_candidate_and_accepts_it(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "高影响偏好"}).json()
    message = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "以后风险都按高等级处理"},
    ).json()

    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/{message['run']['id']}/stream") as response:
        body = response.read().decode("utf-8")

    assert '"type":"memory_candidate_created"' in body
    pending = client.get("/api/agent/memories", params={"status": "pending"}).json()["items"]
    assert len(pending) == 1
    assert pending[0]["riskLevel"] == "high"

    accepted = client.post(f"/api/agent/memory-candidates/{pending[0]['id']}/accept")

    assert accepted.status_code == 200
    assert accepted.json()["memory"]["status"] == "active"
    active = client.get("/api/agent/memories", params={"status": "active"}).json()["items"]
    assert any(item["content"] == pending[0]["content"] for item in active)


def test_memory_list_is_scoped_by_user_header(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    client = TestClient(app)
    first = client.post("/api/agent/sessions", json={"title": "u1"}, headers={"X-Agent-User-Id": "u1"}).json()
    message = client.post(
        f"/api/agent/sessions/{first['session_id']}/messages",
        json={"content": "以后回答短一点"},
        headers={"X-Agent-User-Id": "u1"},
    ).json()
    with client.stream(
        "GET",
        f"/api/agent/sessions/{first['session_id']}/runs/{message['run']['id']}/stream",
        headers={"X-Agent-User-Id": "u1"},
    ) as response:
        response.read()

    assert client.get("/api/agent/memories", headers={"X-Agent-User-Id": "u1"}).json()["items"]
    assert client.get("/api/agent/memories", headers={"X-Agent-User-Id": "u2"}).json()["items"] == []
```

- [ ] **Step 2: Run new API tests and verify failure**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py::test_low_risk_preference_emits_memory_accepted_and_is_recalled tests/test_sessions_api.py::test_high_risk_preference_creates_pending_candidate_and_accepts_it tests/test_sessions_api.py::test_memory_list_is_scoped_by_user_header -q
```

Expected: FAIL because memory event types, routes, and service integration do not exist.

- [ ] **Step 3: Add memory event types**

In `server-agent/app/agent/events.py`, add:

```python
    MEMORY_ACCEPTED = "memory_accepted"
    MEMORY_CANDIDATE_CREATED = "memory_candidate_created"
    MEMORY_RECALLED = "memory_recalled"
```

- [ ] **Step 4: Add request user helper and in-memory stores**

In `server-agent/app/api/sessions.py`, import `preference_memory_service` and add:

```python
from app.services import preference_memory_service

_user_memories: dict[str, list[dict[str, Any]]] = {}
```

Add helpers:

```python
def _user_id_from_request(request: Request | None) -> str:
    if request is None:
        return "system"
    value = request.headers.get("X-Agent-User-Id")
    return value.strip() if value and value.strip() else "system"


async def _list_preference_memories(user_id: str, status: str) -> list[dict[str, Any]]:
    if not _use_in_memory_store():
        return await preference_memory_service.list_memories(_get_repository(), user_id=user_id, status=status)
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
```

Add response normalizers:

```python
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
```

- [ ] **Step 5: Add memory API routes**

In `server-agent/app/api/sessions.py`, add routes near event/list endpoints:

```python
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
    _user_memories.setdefault(user_id, []).append(memory)
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
```

- [ ] **Step 6: Add in-memory extraction and recall helpers**

In `server-agent/app/api/sessions.py`, add:

```python
async def _recall_preference_memories(user_id: str, query: str) -> list[dict[str, Any]]:
    if not _use_in_memory_store():
        return await preference_memory_service.recall_memories(
            _get_repository(),
            user_id=user_id,
            query=query,
        )
    memories = [_memory_response(memory) for memory in _user_memories.get(user_id, []) if memory.get("status") == "active"]
    return preference_memory_service.rank_recalled_memories(memories, query)


async def _store_preference_memories(user_id: str, session_id: str, run_id: str, content: str) -> dict[str, list[dict[str, Any]]]:
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
            "status": "accepted" if candidate.proposed_action == "auto_accept" else "pending",
            "created_at": _now_iso(),
        }
        _memory_candidates[candidate.id] = candidate_record
        if candidate_record["status"] == "accepted":
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
            _user_memories.setdefault(user_id, []).append(memory)
            accepted.append(memory)
        else:
            pending.append(candidate_record)
    return {"accepted": accepted, "pending": pending}
```

- [ ] **Step 7: Emit memory events in streaming paths**

In both in-memory and persistent specific run stream generators, before constructing the runtime service:

```python
        user_id = _user_id_from_request(request)
        recalled_memories = await _recall_preference_memories(user_id, run["input_text"])
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
        service = _build_runtime_service(_accepted_summary_memory(run["input_text"]), recalled_memories)
```

After run completion and before final `run_status`:

```python
        memory_result = await _store_preference_memories(user_id, session_id, run_id, run["input_text"])
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
```

Keep persisted `AgentEvent` seq values unchanged for runtime events. The synthetic memory UI events can use seq `0` or `len(events)+1` for display because they are emitted over SSE for UI feedback in phase 1.

- [ ] **Step 8: Update runtime service and prompt injection**

Change `server-agent/app/agent/service.py` constructor:

```python
    def __init__(
        self,
        tool_registry: ToolRegistry,
        retriever: Retriever | None = None,
        summary_memory: list[str] | None = None,
        preference_memory: list[dict] | None = None,
    ) -> None:
        preference_text = _format_preference_memory(preference_memory or [])
```

Add helper:

```python
def _format_preference_memory(memories: list[dict]) -> list[str]:
    return [str(memory.get("content") or "") for memory in memories if memory.get("content")]
```

Pass both old and new:

```python
            self.orchestrator = ReactRuntime(
                tool_registry=tool_registry,
                llm_client=llm_client,
                retriever=retriever or build_qdrant_retriever(),
                summary_memory=summary_memory or [],
                preference_memory=preference_text,
            )
```

In `server-agent/app/react/runtime.py`, accept and store the new parameter:

```python
        preference_memory: list[str] | None = None,
```

```python
        self._preference_memory = preference_memory or []
```

Update context event payload:

```python
                "preference_memory_count": len(self._preference_memory),
```

Pass both memory groups to `build_action_prompt`:

```python
                preference_memory=self._preference_memory,
```

In `server-agent/app/react/prompts.py`, extend `build_action_prompt` signature:

```python
    preference_memory: list[str] | None = None,
```

and include:

```python
        f"用户长期偏好: {preference_memory or []}\n"
```

- [ ] **Step 9: Add runtime prompt test**

Add to `server-agent/tests/test_react_runtime.py`:

```python
async def test_react_runtime_injects_preference_memory_into_prompt() -> None:
    llm = ScriptedLlm([ReactAction(thought_summary="直接回答。", action="final_answer")])
    runtime = ReactRuntime(
        tool_registry=ToolRegistry([]),
        llm_client=llm,
        retriever=lambda query: [],
        preference_memory=["回答先给结论。"],
    )

    _ = [
        event
        async for event in runtime.run(
            session_id="ana_pref",
            run_id="run_pref",
            raw_input="给出压力波动建议",
            permissions=set(),
        )
    ]

    assert "用户长期偏好" in llm.calls[0]["user"]
    assert "回答先给结论" in llm.calls[0]["user"]
```

- [ ] **Step 10: Run focused backend tests**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_preference_memory_service.py tests/test_react_runtime.py::test_react_runtime_injects_preference_memory_into_prompt tests/test_sessions_api.py::test_low_risk_preference_emits_memory_accepted_and_is_recalled tests/test_sessions_api.py::test_high_risk_preference_creates_pending_candidate_and_accepts_it tests/test_sessions_api.py::test_memory_list_is_scoped_by_user_header -q
```

Expected: PASS.

- [ ] **Step 11: Commit Agent API and runtime changes**

Run:

```bash
git add server-agent/app/agent/events.py server-agent/app/api/sessions.py server-agent/app/agent/service.py server-agent/app/react/runtime.py server-agent/app/react/prompts.py server-agent/tests/test_sessions_api.py server-agent/tests/test_react_runtime.py
git commit -m "feat(agent): expose preference memory events"
```

Expected: commit succeeds with Agent service/runtime/test changes only.

## Task 4: PostgreSQL Init SQL

**Files:**
- Modify: `database/postgres/01-init/agent_init_only_structure.sql`
- Modify: `database/postgres/01-init/agent_init_structure_data.sql`

- [ ] **Step 1: Add idempotent DDL to both Postgres init files**

In both SQL files, after the `memory_candidate` table section and before indexes, add:

```sql
ALTER TABLE "public"."memory_candidate" ADD COLUMN IF NOT EXISTS "user_id" varchar(64) NOT NULL DEFAULT 'system';
ALTER TABLE "public"."memory_candidate" ADD COLUMN IF NOT EXISTS "candidate_type" varchar(64);
ALTER TABLE "public"."memory_candidate" ADD COLUMN IF NOT EXISTS "preference_key" varchar(128);
ALTER TABLE "public"."memory_candidate" ADD COLUMN IF NOT EXISTS "risk_level" varchar(16);
ALTER TABLE "public"."memory_candidate" ADD COLUMN IF NOT EXISTS "proposed_action" varchar(32);
ALTER TABLE "public"."memory_candidate" ADD COLUMN IF NOT EXISTS "source_text" text;
ALTER TABLE "public"."memory_candidate" ADD COLUMN IF NOT EXISTS "reason" text;

CREATE TABLE IF NOT EXISTS "public"."user_memory" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "memory_type" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "preference_key" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "status" varchar(32) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'active',
  "risk_level" varchar(16) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'low',
  "source_candidate_id" varchar(64) COLLATE "pg_catalog"."default",
  "source_session_id" varchar(64) COLLATE "pg_catalog"."default",
  "source_run_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "expires_at" timestamptz(6)
);

COMMENT ON TABLE "public"."user_memory" IS '虚拟专家用户级生效偏好记忆表';
COMMENT ON COLUMN "public"."user_memory"."preference_key" IS '稳定偏好键';
COMMENT ON COLUMN "public"."user_memory"."content" IS '注入 Agent 的自然语言偏好';

ALTER TABLE "public"."user_memory" ADD CONSTRAINT "user_memory_pkey" PRIMARY KEY ("id");
ALTER TABLE "public"."user_memory" ADD CONSTRAINT "ck_user_memory_status" CHECK (status::text = ANY (ARRAY['active'::character varying, 'deleted'::character varying, 'expired'::character varying]::text[]));
ALTER TABLE "public"."user_memory" ADD CONSTRAINT "ck_user_memory_risk_level" CHECK (risk_level::text = ANY (ARRAY['low'::character varying, 'high'::character varying]::text[]));

CREATE INDEX IF NOT EXISTS "idx_user_memory_user_status" ON "public"."user_memory" USING btree ("user_id", "status");
CREATE INDEX IF NOT EXISTS "idx_user_memory_preference_key" ON "public"."user_memory" USING btree ("user_id", "preference_key", "status");
CREATE INDEX IF NOT EXISTS "idx_memory_candidate_user_status" ON "public"."memory_candidate" USING btree ("user_id", "status");
```

- [ ] **Step 2: Validate SQL references**

Run:

```bash
rg -n "user_memory|candidate_type|preference_key|idx_user_memory" database/postgres/01-init
```

Expected: both Postgres init SQL files contain `user_memory`, candidate metadata columns, and indexes.

- [ ] **Step 3: Commit SQL updates**

Run:

```bash
git add database/postgres/01-init/agent_init_only_structure.sql database/postgres/01-init/agent_init_structure_data.sql
git commit -m "chore(agent): add memory v2 postgres schema"
```

Expected: commit succeeds with SQL files only.

## Task 5: Spring Proxy Memory Endpoints

**Files:**
- Modify: `server/server-web/src/main/java/com/gxa/pipe/virtualExpert/agent/AgentClient.java`
- Modify: `server/server-web/src/main/java/com/gxa/pipe/virtualExpert/agent/VirtualExpertAgentController.java`

- [ ] **Step 1: Add AgentClient methods**

In `AgentClient.java`, add:

```java
    public Map<String, Object> listMemories(String status) {
        Map<String, Object> response = restTemplate.exchange(
                url("/api/agent/memories?status={status}"),
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                status == null ? "active" : status
        ).getBody();
        return response == null ? Map.of("items", java.util.List.of()) : response;
    }

    public Map<String, Object> acceptMemoryCandidate(String candidateId) {
        return restTemplate.exchange(
                url("/api/agent/memory-candidates/{candidateId}/accept"),
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                candidateId
        ).getBody();
    }

    public Map<String, Object> rejectMemoryCandidate(String candidateId) {
        return restTemplate.exchange(
                url("/api/agent/memory-candidates/{candidateId}/reject"),
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                candidateId
        ).getBody();
    }

    public Map<String, Object> deleteMemory(String memoryId) {
        return restTemplate.exchange(
                url("/api/agent/memories/{memoryId}"),
                HttpMethod.DELETE,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                memoryId
        ).getBody();
    }
```

- [ ] **Step 2: Forward current user id**

Add a future-safe header in `headers()`:

```java
        headers.set("X-Agent-User-Id", "system");
```

This keeps the first phase user scoped in local/dev behavior. A later auth integration can replace `"system"` with the logged-in user id.

- [ ] **Step 3: Add controller routes**

In `VirtualExpertAgentController.java`, add:

```java
    @GetMapping("/memories")
    public Map<String, Object> listMemories(@RequestParam(value = "status", required = false) String status) {
        return agentClient.listMemories(status);
    }

    @PostMapping("/memory-candidates/{candidateId}/accept")
    public Map<String, Object> acceptMemoryCandidate(@PathVariable String candidateId) {
        return agentClient.acceptMemoryCandidate(candidateId);
    }

    @PostMapping("/memory-candidates/{candidateId}/reject")
    public Map<String, Object> rejectMemoryCandidate(@PathVariable String candidateId) {
        return agentClient.rejectMemoryCandidate(candidateId);
    }

    @DeleteMapping("/memories/{memoryId}")
    public Map<String, Object> deleteMemory(@PathVariable String memoryId) {
        return agentClient.deleteMemory(memoryId);
    }
```

- [ ] **Step 4: Compile server-web**

Run:

```bash
cd server
mvn -pl server-web -am test-compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit Spring proxy**

Run:

```bash
git add server/server-web/src/main/java/com/gxa/pipe/virtualExpert/agent/AgentClient.java server/server-web/src/main/java/com/gxa/pipe/virtualExpert/agent/VirtualExpertAgentController.java
git commit -m "feat(web): proxy agent memory endpoints"
```

Expected: commit succeeds with Spring proxy files only.

## Task 6: Frontend API, Types, And Service Helpers

**Files:**
- Modify: `frontend/src/modules/virtual-expert/types.ts`
- Modify: `frontend/src/modules/virtual-expert/api.ts`
- Modify: `frontend/src/modules/virtual-expert/service.ts`
- Modify: `frontend/src/modules/virtual-expert/service.test.ts`

- [ ] **Step 1: Add memory types**

In `types.ts`, add:

```ts
export type AgentMemoryType = 'preference' | 'export_preference' | 'analysis_preference' | 'interaction_preference'

export type AgentMemoryStatus = 'active' | 'pending' | 'deleted' | 'expired' | 'accepted' | 'rejected'

export type AgentMemoryItem = {
  id: string
  memoryType: AgentMemoryType
  preferenceKey?: string
  content: string
  status: AgentMemoryStatus
  riskLevel?: 'low' | 'high'
  reason?: string
  createdAt?: string
  updatedAt?: string
}

export type AgentMemoriesResponse = {
  items: AgentMemoryItem[]
}

export type AgentMemoryMutationResponse = {
  memory?: AgentMemoryItem
  candidate?: AgentMemoryItem
}
```

- [ ] **Step 2: Add API functions**

In `api.ts`, import new types and add:

```ts
export function fetchAgentMemories(status: 'active' | 'pending' = 'active') {
  return apiRequest<AgentMemoriesResponse>('/manager/virtual-expert/agent/memories', {
    query: { status },
  })
}

export function acceptAgentMemoryCandidate(candidateId: string) {
  return apiRequest<AgentMemoryMutationResponse>(
    `/manager/virtual-expert/agent/memory-candidates/${encodeURIComponent(candidateId)}/accept`,
    { method: 'POST' },
  )
}

export function rejectAgentMemoryCandidate(candidateId: string) {
  return apiRequest<AgentMemoryMutationResponse>(
    `/manager/virtual-expert/agent/memory-candidates/${encodeURIComponent(candidateId)}/reject`,
    { method: 'POST' },
  )
}

export function deleteAgentMemory(memoryId: string) {
  return apiRequest<AgentMemoryMutationResponse>(
    `/manager/virtual-expert/agent/memories/${encodeURIComponent(memoryId)}`,
    { method: 'DELETE' },
  )
}
```

- [ ] **Step 3: Add service helpers and labels**

In `service.ts`, import API functions and add:

```ts
export function memoryTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    preference: '输出偏好',
    export_preference: '导出偏好',
    analysis_preference: '分析偏好',
    interaction_preference: '交互偏好',
  }
  return labels[type || ''] || '偏好'
}

export function normalizeAgentMemoryItem(raw: Record<string, unknown>): AgentMemoryItem {
  return {
    id: String(raw.id ?? raw.candidateId ?? raw.memoryId ?? ''),
    memoryType: String(raw.memoryType ?? raw.memory_type ?? 'preference') as AgentMemoryType,
    preferenceKey: typeof raw.preferenceKey === 'string' ? raw.preferenceKey : typeof raw.preference_key === 'string' ? raw.preference_key : undefined,
    content: String(raw.content ?? ''),
    status: String(raw.status ?? 'active') as AgentMemoryStatus,
    riskLevel: raw.riskLevel === 'high' || raw.risk_level === 'high' ? 'high' : 'low',
    reason: typeof raw.reason === 'string' ? raw.reason : undefined,
    createdAt: typeof raw.createdAt === 'string' ? raw.createdAt : typeof raw.created_at === 'string' ? raw.created_at : undefined,
    updatedAt: typeof raw.updatedAt === 'string' ? raw.updatedAt : typeof raw.updated_at === 'string' ? raw.updated_at : undefined,
  }
}

export function memoryNoticeFromEvent(event: AgentEvent): AgentMemoryItem | undefined {
  if (!['memory_accepted', 'memory_candidate_created', 'memory_recalled'].includes(event.type)) {
    return undefined
  }
  return normalizeAgentMemoryItem(event.payload || {})
}

export async function loadAgentMemories(status: 'active' | 'pending' = 'active') {
  const response = await fetchAgentMemories(status)
  return response.items.map((item) => normalizeAgentMemoryItem(item as unknown as Record<string, unknown>))
}

export async function requestAgentMemoryAccept(candidateId: string) {
  const response = await acceptAgentMemoryCandidate(candidateId)
  return response.memory ? normalizeAgentMemoryItem(response.memory as unknown as Record<string, unknown>) : undefined
}

export async function requestAgentMemoryReject(candidateId: string) {
  const response = await rejectAgentMemoryCandidate(candidateId)
  return response.candidate ? normalizeAgentMemoryItem(response.candidate as unknown as Record<string, unknown>) : undefined
}

export async function requestAgentMemoryDelete(memoryId: string) {
  const response = await deleteAgentMemory(memoryId)
  return response.memory ? normalizeAgentMemoryItem(response.memory as unknown as Record<string, unknown>) : undefined
}
```

- [ ] **Step 4: Add service tests**

In `service.test.ts`, import new helpers and add:

```ts
describe('memory helpers', () => {
  it('normalizes memory accepted payloads', () => {
    const item = normalizeAgentMemoryItem({
      memoryId: 'mem_1',
      memoryType: 'preference',
      content: '回答先给结论。',
      riskLevel: 'low',
    })

    expect(item).toMatchObject({
      id: 'mem_1',
      memoryType: 'preference',
      content: '回答先给结论。',
      riskLevel: 'low',
    })
  })

  it('extracts a memory notice from stream events', () => {
    const notice = memoryNoticeFromEvent(event(8, 'memory_candidate_created', {
      candidateId: 'cand_1',
      content: '以后风险都按高等级处理',
      riskLevel: 'high',
      reason: '需要确认',
    }))

    expect(notice).toMatchObject({
      id: 'cand_1',
      content: '以后风险都按高等级处理',
      riskLevel: 'high',
      reason: '需要确认',
    })
  })

  it('labels known memory types in Chinese', () => {
    expect(memoryTypeLabel('export_preference')).toBe('导出偏好')
    expect(memoryTypeLabel('interaction_preference')).toBe('交互偏好')
  })
})
```

- [ ] **Step 5: Run frontend service tests**

Run:

```bash
cd frontend
pnpm exec vitest run src/modules/virtual-expert/service.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit frontend API/service helpers**

Run:

```bash
git add frontend/src/modules/virtual-expert/types.ts frontend/src/modules/virtual-expert/api.ts frontend/src/modules/virtual-expert/service.ts frontend/src/modules/virtual-expert/service.test.ts
git commit -m "feat(frontend): add agent memory service helpers"
```

Expected: commit succeeds with frontend API/service/test changes only.

## Task 7: Frontend Visible Memory UI

**Files:**
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue`

- [ ] **Step 1: Add state and imports**

In `VirtualExpertPage.vue`, import helpers:

```ts
import {
  loadAgentMemories,
  memoryNoticeFromEvent,
  memoryTypeLabel,
  requestAgentMemoryAccept,
  requestAgentMemoryDelete,
  requestAgentMemoryReject,
} from '../service'
import type { AgentMemoryItem } from '../types'
```

Add refs near other state:

```ts
const memoryPanelOpen = ref(false)
const activeMemories = ref<AgentMemoryItem[]>([])
const pendingMemories = ref<AgentMemoryItem[]>([])
const memoryNotices = ref<AgentMemoryItem[]>([])
const recalledMemorySummary = ref('')
```

- [ ] **Step 2: Add memory loading and mutation methods**

Add methods:

```ts
async function refreshMemories() {
  try {
    const [active, pending] = await Promise.all([
      loadAgentMemories('active'),
      loadAgentMemories('pending'),
    ])
    activeMemories.value = active
    pendingMemories.value = pending
  } catch {
    activeMemories.value = []
    pendingMemories.value = []
  }
}

async function acceptMemory(item: AgentMemoryItem) {
  const accepted = await requestAgentMemoryAccept(item.id)
  pendingMemories.value = pendingMemories.value.filter((memory) => memory.id !== item.id)
  if (accepted) {
    activeMemories.value = [accepted, ...activeMemories.value.filter((memory) => memory.id !== accepted.id)]
  }
}

async function rejectMemory(item: AgentMemoryItem) {
  await requestAgentMemoryReject(item.id)
  pendingMemories.value = pendingMemories.value.filter((memory) => memory.id !== item.id)
}

async function removeMemory(item: AgentMemoryItem) {
  await requestAgentMemoryDelete(item.id)
  activeMemories.value = activeMemories.value.filter((memory) => memory.id !== item.id)
}

function rememberNoticeFromEvent(event: AgentEvent) {
  const notice = memoryNoticeFromEvent(event)
  if (!notice) {
    return
  }
  if (event.type === 'memory_recalled') {
    const items = Array.isArray(event.payload?.items) ? event.payload.items : []
    recalledMemorySummary.value = items
      .map((item) => String((item as Record<string, unknown>).content ?? ''))
      .filter(Boolean)
      .slice(0, 2)
      .join('、')
    return
  }
  memoryNotices.value = [notice, ...memoryNotices.value.filter((item) => item.id !== notice.id)].slice(0, 6)
  if (event.type === 'memory_candidate_created') {
    pendingMemories.value = [notice, ...pendingMemories.value.filter((item) => item.id !== notice.id)]
  }
  if (event.type === 'memory_accepted') {
    activeMemories.value = [notice, ...activeMemories.value.filter((item) => item.id !== notice.id)]
  }
}
```

Call `refreshMemories()` in the existing mount/load initialization after sessions load.

- [ ] **Step 3: Wire stream events into memory state**

In `updateCurrentRun(event)` or immediately before `applyStreamEvent`, add:

```ts
  rememberNoticeFromEvent(event)
```

This makes memory cards appear live while the SSE stream is running.

- [ ] **Step 4: Add sidebar memory entry**

In the sidebar after the "开启新对话" button, add:

```vue
        <button class="memory-entry" type="button" @click="memoryPanelOpen = true">
          <span>我的偏好</span>
          <strong>{{ activeMemories.length }}</strong>
          <small v-if="pendingMemories.length">{{ pendingMemories.length }} 条待确认</small>
        </button>
```

- [ ] **Step 5: Add conversation memory notices**

Inside `.conversation-scroll`, before the turn loop, add:

```vue
          <section v-if="recalledMemorySummary" class="memory-banner">
            <span class="chip">记忆召回</span>
            <p>本轮已参考你的偏好：{{ recalledMemorySummary }}</p>
          </section>

          <section v-if="memoryNotices.length" class="memory-notices">
            <article
              v-for="notice in memoryNotices"
              :key="notice.id"
              class="memory-notice"
              :class="{ 'is-pending': notice.status === 'pending' || notice.riskLevel === 'high' }"
            >
              <div>
                <span class="chip">{{ memoryTypeLabel(notice.memoryType) }}</span>
                <strong>{{ notice.riskLevel === 'high' ? '这个偏好需要确认' : '已记住偏好' }}</strong>
                <p>{{ notice.content }}</p>
                <small v-if="notice.reason">{{ notice.reason }}</small>
              </div>
              <div v-if="notice.riskLevel === 'high'" class="memory-actions">
                <button class="ghost-action" @click="acceptMemory(notice)">记住</button>
                <button class="ghost-action" @click="rejectMemory(notice)">忽略</button>
              </div>
            </article>
          </section>
```

- [ ] **Step 6: Add memory drawer**

Near the end of the template inside `ModuleShell`, add:

```vue
    <aside v-if="memoryPanelOpen" class="memory-panel">
      <div class="memory-panel__head">
        <div>
          <span class="eyebrow">MEMORY</span>
          <h3>我的偏好</h3>
        </div>
        <button class="ghost-action" @click="memoryPanelOpen = false">关闭</button>
      </div>

      <section v-if="pendingMemories.length" class="memory-section">
        <h4>待确认</h4>
        <article v-for="item in pendingMemories" :key="item.id" class="memory-card is-pending">
          <span class="chip">{{ memoryTypeLabel(item.memoryType) }}</span>
          <p>{{ item.content }}</p>
          <small>{{ item.reason || '该偏好会影响后续行为，请确认是否记住。' }}</small>
          <div class="memory-actions">
            <button class="ghost-action" @click="acceptMemory(item)">记住</button>
            <button class="ghost-action" @click="rejectMemory(item)">忽略</button>
          </div>
        </article>
      </section>

      <section class="memory-section">
        <h4>已生效</h4>
        <article v-for="item in activeMemories" :key="item.id" class="memory-card">
          <span class="chip">{{ memoryTypeLabel(item.memoryType) }}</span>
          <p>{{ item.content }}</p>
          <button class="ghost-action" @click="removeMemory(item)">删除</button>
        </article>
        <p v-if="!activeMemories.length" class="memory-empty">还没有保存偏好。</p>
      </section>
    </aside>
```

- [ ] **Step 7: Add CSS with stable dimensions**

Add CSS near existing agent sidebar styles:

```css
.memory-entry {
  width: 100%;
  min-height: 52px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(15, 23, 42, 0.34);
  color: inherit;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 10px;
  align-items: center;
  padding: 10px 12px;
  text-align: left;
}

.memory-entry small {
  grid-column: 1 / -1;
  color: rgba(226, 232, 240, 0.7);
}

.memory-banner,
.memory-notice,
.memory-card {
  border: 1px solid rgba(56, 189, 248, 0.28);
  background: rgba(8, 47, 73, 0.24);
  border-radius: 8px;
  padding: 12px;
}

.memory-notices {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.memory-notice {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.memory-notice.is-pending,
.memory-card.is-pending {
  border-color: rgba(250, 204, 21, 0.38);
  background: rgba(113, 63, 18, 0.22);
}

.memory-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.memory-panel {
  position: fixed;
  right: 24px;
  top: 72px;
  bottom: 24px;
  width: min(420px, calc(100vw - 32px));
  z-index: 40;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(15, 23, 42, 0.96);
  box-shadow: 0 24px 80px rgba(2, 6, 23, 0.5);
  padding: 16px;
  overflow: auto;
}

.memory-panel__head,
.memory-section {
  display: grid;
  gap: 12px;
}

.memory-section {
  margin-top: 18px;
}

.memory-card {
  display: grid;
  gap: 8px;
}

.memory-empty {
  color: rgba(226, 232, 240, 0.68);
}
```

- [ ] **Step 8: Run frontend checks**

Run:

```bash
cd frontend
pnpm exec vitest run src/modules/virtual-expert/service.test.ts
pnpm build
```

Expected: service tests PASS and build succeeds.

- [ ] **Step 9: Commit frontend visible memory UI**

Run:

```bash
git add frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue
git commit -m "feat(frontend): show agent preference memory"
```

Expected: commit succeeds with the Vue page only.

## Task 8: Full Verification And Final Review

**Files:**
- No new source files expected.

- [ ] **Step 1: Run focused Agent backend tests**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_preference_memory_service.py tests/test_sessions_api.py tests/test_react_runtime.py tests/test_persistence_models.py -q
```

Expected: PASS.

- [ ] **Step 2: Run all Agent tests**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest -q
```

Expected: PASS.

- [ ] **Step 3: Run Spring compile check**

Run:

```bash
cd server
mvn -pl server-web -am test-compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Run frontend verification**

Run:

```bash
cd frontend
pnpm exec vitest run
pnpm build
```

Expected: Vitest PASS and Vite build succeeds.

- [ ] **Step 5: Manual local smoke test**

With local services running:

1. Open `http://localhost:5173/virtual-expert`.
2. Send `以后回答先给结论`.
3. Confirm the conversation shows `已记住偏好：以后回答先给结论`.
4. Send `压力波动怎么处理`.
5. Confirm the conversation or process area shows `本轮已参考你的偏好`.
6. Send `以后风险都按高等级处理`.
7. Confirm a pending memory card appears with `记住` and `忽略`.
8. Click `记住`.
9. Open `我的偏好`.
10. Confirm both accepted preferences appear and can be deleted.

- [ ] **Step 6: Inspect diff and staged state**

Run:

```bash
git status --short
git diff --check
```

Expected: no whitespace errors. Any remaining unstaged files are either expected local user changes or files deliberately left for a later task.

- [ ] **Step 7: Confirm no uncommitted Memory v2 files remain**

Run:

```bash
git status --short server-agent/app server-agent/tests server/server-web/src/main/java/com/gxa/pipe/virtualExpert/agent frontend/src/modules/virtual-expert database/postgres/01-init
```

Expected: no unstaged or staged Memory v2 files remain. If this command prints files, inspect them and either commit the specific fix in the task that introduced it or remove the accidental change before finishing.
