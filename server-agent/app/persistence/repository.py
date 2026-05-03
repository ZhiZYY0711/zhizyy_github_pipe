from collections.abc import Sequence
from datetime import UTC, datetime
import re
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.agent.events import AgentEvent
from app.agent.state import RunStatus, SessionStatus
from app.persistence.models import (
    AgentEventModel,
    AgentMessageModel,
    AgentRunModel,
    AgentRunSummaryModel,
    AnalysisSessionModel,
    MemoryCandidateModel,
    UserMemoryModel,
)


class AgentRepository:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    @classmethod
    def from_database_url(cls, database_url: str) -> "AgentRepository":
        engine = create_async_engine(database_url)
        return cls(async_sessionmaker(engine, expire_on_commit=False))

    async def create_session(
        self,
        session_id: str,
        request: dict[str, Any],
        user_id: str = "system",
    ) -> dict[str, str]:
        raw_input = request.get("raw_input") or request.get("title") or "新研判会话"
        async with self._session_factory() as db:
            db.add(
                AnalysisSessionModel(
                    id=session_id,
                    title=str(raw_input)[:200],
                    status="created",
                    raw_input=request.get("raw_input"),
                    source_type=request.get("source_type") or "manual",
                    source_id=request.get("source_id"),
                    object_type=request.get("object_type"),
                    object_id=request.get("object_id"),
                    object_name=request.get("object_name"),
                    user_id=user_id,
                )
            )
            await db.commit()
        return {"session_id": session_id, "status": "created"}

    async def get_session(self, session_id: str) -> dict[str, Any] | None:
        async with self._session_factory() as db:
            session = await db.get(AnalysisSessionModel, session_id)
            if session is None:
                return None
            return {
                "session_id": session.id,
                "status": session.status,
                "raw_input": session.raw_input,
                "run_id": session.current_run_id,
                "title": session.title,
            }

    async def list_sessions(self, limit: int = 20) -> Sequence[dict[str, Any]]:
        async with self._session_factory() as db:
            result = await db.execute(
                select(AnalysisSessionModel)
                .order_by(AnalysisSessionModel.updated_at.desc(), AnalysisSessionModel.created_at.desc())
                .limit(max(1, min(limit, 100)))
            )
            return [_session_to_dict(session) for session in result.scalars()]

    async def delete_session(self, session_id: str) -> bool:
        async with self._session_factory() as db:
            session = await db.get(AnalysisSessionModel, session_id)
            if session is None:
                return False
            await db.delete(session)
            await db.commit()
            return True

    async def create_message(
        self,
        message_id: str,
        session_id: str,
        role: str,
        content: str,
        message_type: str = "text",
        triggered_run_id: str | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        async with self._session_factory() as db:
            message = AgentMessageModel(
                id=message_id,
                session_id=session_id,
                role=role,
                content=content,
                message_type=message_type,
                triggered_run_id=triggered_run_id,
                metadata_json=metadata or {},
            )
            db.add(message)
            await db.commit()
            return _message_to_dict(message)

    async def find_active_run(self, session_id: str) -> dict[str, Any] | None:
        async with self._session_factory() as db:
            result = await db.execute(
                select(AgentRunModel)
                .where(
                    AgentRunModel.session_id == session_id,
                    AgentRunModel.status.in_(["created", "running"]),
                )
                .order_by(AgentRunModel.created_at.desc())
                .limit(1)
            )
            run = result.scalar_one_or_none()
            return _run_to_dict(run) if run is not None else None

    async def create_run(
        self,
        session_id: str,
        run_id: str,
        triggering_message_id: str | None = None,
        input_text: str | None = None,
    ) -> None:
        async with self._session_factory() as db:
            db.add(
                AgentRunModel(
                    id=run_id,
                    session_id=session_id,
                    triggering_message_id=triggering_message_id,
                    input_text=input_text,
                    status="created",
                )
            )
            session = await db.get(AnalysisSessionModel, session_id)
            if session is not None:
                session.status = "running"
                session.current_run_id = run_id
                session.updated_at = func.now()
            await db.commit()

    async def start_run(self, session_id: str, run_id: str) -> None:
        async with self._session_factory() as db:
            run = await db.get(AgentRunModel, run_id)
            if run is not None and run.session_id == session_id:
                run.status = "running"
                run.started_at = func.now()
                run.updated_at = func.now()
            session = await db.get(AnalysisSessionModel, session_id)
            if session is not None:
                session.status = "running"
                session.current_run_id = run_id
                session.updated_at = func.now()
            await db.commit()

    async def get_run(self, session_id: str, run_id: str) -> dict[str, Any] | None:
        async with self._session_factory() as db:
            run = await db.get(AgentRunModel, run_id)
            if run is None or run.session_id != session_id:
                return None
            return _run_to_dict(run)

    async def append_event(self, event: AgentEvent) -> None:
        async with self._session_factory() as db:
            db.add(
                AgentEventModel(
                    id=event.id,
                    session_id=event.session_id,
                    run_id=event.run_id,
                    seq=event.seq,
                    type=event.type.value,
                    level=event.level,
                    title=event.title,
                    message=event.message,
                    payload=event.payload,
                    created_at=event.created_at,
                )
            )
            await db.commit()

    async def set_session_status(self, session_id: str, status: SessionStatus) -> None:
        async with self._session_factory() as db:
            session = await db.get(AnalysisSessionModel, session_id)
            if session is not None:
                session.status = status
            await db.commit()

    async def complete_run(
        self,
        session_id: str,
        run_id: str,
        status: RunStatus,
        failure_reason: str | None = None,
    ) -> None:
        async with self._session_factory() as db:
            run = await db.get(AgentRunModel, run_id)
            if run is not None:
                run.status = status
                run.finished_at = func.now()
                run.updated_at = func.now()
                if failure_reason:
                    run.error_message = failure_reason
            session = await db.get(AnalysisSessionModel, session_id)
            if session is not None:
                session.status = _session_status_from_run_status(status)
                session.current_run_id = None
                if status in {"completed", "failed", "cancelled"}:
                    session.completed_at = func.now()
                session.updated_at = func.now()
            await db.commit()

    async def create_run_summary(
        self,
        summary_id: str,
        session_id: str,
        run_id: str,
        summary: dict[str, Any],
        triggering_message_id: str | None = None,
    ) -> dict[str, Any]:
        async with self._session_factory() as db:
            model = AgentRunSummaryModel(
                id=summary_id,
                session_id=session_id,
                run_id=run_id,
                triggering_message_id=triggering_message_id,
                final_answer=summary.get("final_answer"),
                risk_level=summary.get("risk_level"),
                judgment=summary.get("judgment"),
                recommended_actions=summary.get("recommended_actions") or [],
                key_evidence=summary.get("key_evidence") or [],
                open_questions=summary.get("open_questions") or [],
                tool_summary=summary.get("tool_summary") or [],
                knowledge_summary=summary.get("knowledge_summary") or [],
            )
            db.add(model)
            session = await db.get(AnalysisSessionModel, session_id)
            if session is not None:
                session.summary = summary.get("judgment") or summary.get("final_answer")
                session.updated_at = func.now()
            await db.commit()
            return _summary_to_dict(model)

    async def list_events(self, session_id: str, after_seq: int = 0) -> Sequence[dict[str, Any]]:
        async with self._session_factory() as db:
            result = await db.execute(
                select(AgentEventModel)
                .where(AgentEventModel.session_id == session_id, AgentEventModel.seq > after_seq)
                .order_by(AgentEventModel.seq)
            )
            return [
                _event_to_dict(event)
                for event in result.scalars()
            ]

    async def list_run_events(self, session_id: str, run_id: str, after_seq: int = 0) -> Sequence[dict[str, Any]]:
        async with self._session_factory() as db:
            result = await db.execute(
                select(AgentEventModel)
                .where(
                    AgentEventModel.session_id == session_id,
                    AgentEventModel.run_id == run_id,
                    AgentEventModel.seq > after_seq,
                )
                .order_by(AgentEventModel.seq)
            )
            return [_event_to_dict(event) for event in result.scalars()]

    async def list_recent_context(self, session_id: str, message_limit: int = 6, summary_limit: int = 3) -> dict[str, Any]:
        async with self._session_factory() as db:
            message_result = await db.execute(
                select(AgentMessageModel)
                .where(AgentMessageModel.session_id == session_id)
                .order_by(AgentMessageModel.created_at.desc(), AgentMessageModel.id.desc())
                .limit(message_limit)
            )
            summary_result = await db.execute(
                select(AgentRunSummaryModel)
                .where(AgentRunSummaryModel.session_id == session_id)
                .order_by(AgentRunSummaryModel.created_at.desc(), AgentRunSummaryModel.id.desc())
                .limit(summary_limit)
            )
            return {
                "messages": [_message_to_dict(message) for message in reversed(list(message_result.scalars()))],
                "run_summaries": [_summary_to_dict(summary) for summary in reversed(list(summary_result.scalars()))],
            }

    async def list_accepted_memories(self, query: str, limit: int = 5) -> list[str]:
        normalized_query = (query or "").lower()
        async with self._session_factory() as db:
            result = await db.execute(
                select(MemoryCandidateModel)
                .where(MemoryCandidateModel.status == "accepted")
                .order_by(MemoryCandidateModel.created_at.desc())
                .limit(100)
            )
            scored: list[tuple[int, str]] = []
            for memory in result.scalars():
                score = 1 if memory.memory_type in {"preference", "export_preference"} else 0
                for token in _memory_tokens(memory.content):
                    if token in normalized_query:
                        score += 2
                if score > 0:
                    scored.append((score, f"[{memory.memory_type}] {memory.content}"))
            scored.sort(key=lambda item: item[0], reverse=True)
            return [content for _, content in scored[:limit]]

    async def create_memory_candidate(
        self,
        *,
        memory_id: str,
        session_id: str,
        run_id: str,
        memory_type: str,
        title: str,
        content: str,
        status: str = "pending",
        confidence: float | None = None,
        evidence_ids: list[Any] | None = None,
    ) -> None:
        async with self._session_factory() as db:
            db.add(
                MemoryCandidateModel(
                    id=memory_id,
                    session_id=session_id,
                    run_id=run_id,
                    memory_type=memory_type,
                    title=title[:200],
                    content=content,
                    status=status,
                    confidence=confidence,
                    evidence_ids=evidence_ids,
                )
            )
            await db.commit()

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
            candidate.reviewed_at = datetime.now(UTC)
            await db.commit()
            return _memory_candidate_to_dict(candidate)

    async def delete_user_memory(self, user_id: str, memory_id: str) -> dict[str, Any] | None:
        async with self._session_factory() as db:
            memory = await db.get(UserMemoryModel, memory_id)
            if memory is None or memory.user_id != user_id:
                return None
            memory.status = "deleted"
            memory.updated_at = datetime.now(UTC)
            await db.commit()
            return _user_memory_to_dict(memory)

    async def list_timeline(self, session_id: str, before_cursor: str | None = None, limit: int = 1) -> dict[str, Any]:
        limit = max(1, min(limit, 20))
        async with self._session_factory() as db:
            messages = await _select_timeline_messages(db, session_id, before_cursor, limit + 1)
            has_more = len(messages) > limit
            selected = messages[:limit]
            selected.reverse()
            items = []
            for message in selected:
                run = await _get_run_by_message(db, session_id, message.id)
                summary = await _get_summary_by_run(db, session_id, run.id) if run is not None else None
                event_count = await _count_events_by_run(db, session_id, run.id) if run is not None else 0
                items.append(_timeline_item(message, run, summary, event_count))

            return {
                "session_id": session_id,
                "items": items,
                "has_more_before": has_more,
                "before_cursor": items[0]["cursor"] if items else before_cursor,
            }


def _session_status_from_run_status(status: RunStatus) -> SessionStatus:
    if status in {"completed", "failed", "cancelled", "awaiting_user"}:
        return status
    raise ValueError(f"cannot derive session status from run status: {status}")


def _memory_tokens(content: str) -> list[str]:
    return re.findall(r"[a-z0-9][a-z0-9_-]{1,}", (content or "").lower())


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


def _message_to_dict(message: AgentMessageModel) -> dict[str, Any]:
    return {
        "id": message.id,
        "session_id": message.session_id,
        "role": message.role,
        "content": message.content,
        "message_type": message.message_type,
        "triggered_run_id": message.triggered_run_id,
        "metadata": message.metadata_json or {},
        "created_at": message.created_at.isoformat() if message.created_at is not None else None,
    }


def _session_to_dict(session: AnalysisSessionModel) -> dict[str, Any]:
    return {
        "id": session.id,
        "title": session.title,
        "status": session.status,
        "summary": session.summary,
        "source_type": session.source_type,
        "source_id": session.source_id,
        "object_type": session.object_type,
        "object_id": session.object_id,
        "object_name": session.object_name,
        "created_at": session.created_at.isoformat() if session.created_at is not None else None,
        "updated_at": session.updated_at.isoformat() if session.updated_at is not None else None,
    }


def _run_to_dict(run: AgentRunModel) -> dict[str, Any]:
    return {
        "id": run.id,
        "session_id": run.session_id,
        "triggering_message_id": run.triggering_message_id,
        "input_text": run.input_text,
        "status": run.status,
        "created_at": run.created_at.isoformat() if run.created_at is not None else None,
        "updated_at": run.updated_at.isoformat() if run.updated_at is not None else None,
        "finished_at": run.finished_at.isoformat() if run.finished_at is not None else None,
    }


def _summary_to_dict(summary: AgentRunSummaryModel) -> dict[str, Any]:
    return {
        "id": summary.id,
        "session_id": summary.session_id,
        "run_id": summary.run_id,
        "triggering_message_id": summary.triggering_message_id,
        "final_answer": summary.final_answer,
        "risk_level": summary.risk_level,
        "judgment": summary.judgment,
        "recommended_actions": summary.recommended_actions or [],
        "key_evidence": summary.key_evidence or [],
        "open_questions": summary.open_questions or [],
        "tool_summary": summary.tool_summary or [],
        "knowledge_summary": summary.knowledge_summary or [],
        "created_at": summary.created_at.isoformat() if summary.created_at is not None else None,
    }


def _event_to_dict(event: AgentEventModel) -> dict[str, Any]:
    return {
        "id": event.id,
        "session_id": event.session_id,
        "run_id": event.run_id,
        "seq": event.seq,
        "type": event.type,
        "level": event.level,
        "title": event.title,
        "message": event.message,
        "payload": event.payload or {},
        "created_at": event.created_at.isoformat(),
    }


async def _select_timeline_messages(
    db: AsyncSession,
    session_id: str,
    before_cursor: str | None,
    limit: int,
) -> list[AgentMessageModel]:
    statement = (
        select(AgentMessageModel)
        .where(AgentMessageModel.session_id == session_id, AgentMessageModel.role == "user")
        .order_by(AgentMessageModel.created_at.desc(), AgentMessageModel.id.desc())
        .limit(limit)
    )
    if before_cursor:
        created_at_text, message_id = before_cursor.split("|", 1)
        created_at = datetime.fromisoformat(created_at_text)
        statement = (
            select(AgentMessageModel)
            .where(
                AgentMessageModel.session_id == session_id,
                AgentMessageModel.role == "user",
                (
                    (AgentMessageModel.created_at < created_at)
                    | ((AgentMessageModel.created_at == created_at) & (AgentMessageModel.id < message_id))
                ),
            )
            .order_by(AgentMessageModel.created_at.desc(), AgentMessageModel.id.desc())
            .limit(limit)
        )
    result = await db.execute(statement)
    return list(result.scalars())


async def _get_run_by_message(db: AsyncSession, session_id: str, message_id: str) -> AgentRunModel | None:
    result = await db.execute(
        select(AgentRunModel)
        .where(AgentRunModel.session_id == session_id, AgentRunModel.triggering_message_id == message_id)
        .order_by(AgentRunModel.created_at.desc(), AgentRunModel.id.desc())
        .limit(1)
    )
    return result.scalar_one_or_none()


async def _get_summary_by_run(db: AsyncSession, session_id: str, run_id: str) -> AgentRunSummaryModel | None:
    result = await db.execute(
        select(AgentRunSummaryModel)
        .where(AgentRunSummaryModel.session_id == session_id, AgentRunSummaryModel.run_id == run_id)
        .limit(1)
    )
    return result.scalar_one_or_none()


async def _count_events_by_run(db: AsyncSession, session_id: str, run_id: str) -> int:
    result = await db.execute(
        select(func.count())
        .select_from(AgentEventModel)
        .where(AgentEventModel.session_id == session_id, AgentEventModel.run_id == run_id)
    )
    return int(result.scalar() or 0)


def _timeline_item(
    message: AgentMessageModel,
    run: AgentRunModel | None,
    summary: AgentRunSummaryModel | None,
    event_count: int,
) -> dict[str, Any]:
    run_payload = None
    if run is not None:
        run_payload = {
            "id": run.id,
            "status": run.status,
            "summary": _summary_payload(summary),
            "event_count": event_count,
            "created_at": run.created_at.isoformat() if run.created_at is not None else None,
            "completed_at": run.finished_at.isoformat() if run.finished_at is not None else None,
        }
    return {
        "turn_id": message.id,
        "cursor": f"{message.created_at.isoformat()}|{message.id}",
        "user_message": {
            "id": message.id,
            "content": message.content,
            "created_at": message.created_at.isoformat() if message.created_at is not None else None,
        },
        "run": run_payload,
    }


def _summary_payload(summary: AgentRunSummaryModel | None) -> dict[str, Any] | None:
    if summary is None:
        return None
    return {
        "final_answer": summary.final_answer,
        "risk_level": summary.risk_level,
        "judgment": summary.judgment,
        "recommended_actions": summary.recommended_actions or [],
        "key_evidence": summary.key_evidence or [],
        "open_questions": summary.open_questions or [],
    }
