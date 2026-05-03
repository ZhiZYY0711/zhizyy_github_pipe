import re
from datetime import UTC, datetime

import pytest

from app.core.config import Settings
from app.persistence.models import (
    AgentEventModel,
    AgentMessageModel,
    MemoryCandidateModel,
    AgentRunModel,
    AgentRunSummaryModel,
    AnalysisSessionModel,
    UserMemoryModel,
)
from app.persistence.repository import AgentRepository


def test_settings_enable_in_memory_store_from_env(monkeypatch):
    monkeypatch.setenv("AGENT_USE_IN_MEMORY_STORE", "true")

    settings = Settings()

    assert settings.use_in_memory_store is True


def test_persistence_models_match_core_table_names_columns_and_status_checks():
    assert AnalysisSessionModel.__tablename__ == "analysis_session"
    assert AgentMessageModel.__tablename__ == "agent_message"
    assert AgentRunModel.__tablename__ == "agent_run"
    assert AgentRunSummaryModel.__tablename__ == "agent_run_summary"
    assert AgentEventModel.__tablename__ == "agent_event"
    assert MemoryCandidateModel.__tablename__ == "memory_candidate"

    assert "raw_input" in AnalysisSessionModel.__table__.columns
    assert "current_run_id" in AnalysisSessionModel.__table__.columns
    assert "triggering_message_id" in AgentRunModel.__table__.columns
    assert "input_text" in AgentRunModel.__table__.columns
    assert "state_snapshot" in AgentRunModel.__table__.columns
    assert "role" in AgentMessageModel.__table__.columns
    assert "final_answer" in AgentRunSummaryModel.__table__.columns
    assert "payload" in AgentEventModel.__table__.columns
    assert "memory_type" in MemoryCandidateModel.__table__.columns
    assert "confidence" in MemoryCandidateModel.__table__.columns
    assert "status" in MemoryCandidateModel.__table__.columns

    session_status_constraint = next(
        constraint
        for constraint in AnalysisSessionModel.__table__.constraints
        if constraint.name == "ck_analysis_session_status"
    )
    run_status_constraint = next(
        constraint
        for constraint in AgentRunModel.__table__.constraints
        if constraint.name == "ck_agent_run_status"
    )

    session_status_sql = str(session_status_constraint.sqltext)
    run_status_sql = str(run_status_constraint.sqltext)
    session_statuses = set(re.findall(r"'([^']+)'", session_status_sql))
    run_statuses = set(re.findall(r"'([^']+)'", run_status_sql))

    for expected_status in [
        "created",
        "running",
        "awaiting_user",
        "completed",
        "failed",
        "cancelled",
        "archived",
    ]:
        assert expected_status in session_statuses

    for expected_status in [
        "created",
        "running",
        "understanding",
        "context_building",
        "planning",
        "retrieving",
        "tool_running",
        "reasoning",
        "awaiting_user",
        "completed",
        "failed",
        "cancelled",
    ]:
        assert expected_status in run_statuses

    assert AnalysisSessionModel.__table__.c.status.nullable is False
    assert AgentRunModel.__table__.c.status.nullable is False
    assert AnalysisSessionModel.__table__.c.status.default.arg == "created"
    assert AgentRunModel.__table__.c.status.default.arg == "created"


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


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("run_status", "expected_session_status"),
    [
        ("completed", "completed"),
        ("failed", "failed"),
        ("cancelled", "cancelled"),
        ("awaiting_user", "awaiting_user"),
    ],
)
async def test_repository_finalizes_session_status_from_run_status(
    run_status: str,
    expected_session_status: str,
):
    class FakeDb:
        def __init__(self) -> None:
            self.sessions: dict[str, AnalysisSessionModel] = {}
            self.runs: dict[str, AgentRunModel] = {}
            self.events: dict[str, AgentEventModel] = {}

        async def __aenter__(self) -> "FakeDb":
            return self

        async def __aexit__(self, exc_type, exc, tb) -> None:
            return None

        def add(self, obj) -> None:
            if isinstance(obj, AnalysisSessionModel):
                self.sessions[obj.id] = obj
            elif isinstance(obj, AgentRunModel):
                self.runs[obj.id] = obj
            elif isinstance(obj, AgentEventModel):
                self.events[obj.id] = obj

        async def commit(self) -> None:
            return None

        async def get(self, model, key):
            if model is AnalysisSessionModel:
                return self.sessions.get(key)
            if model is AgentRunModel:
                return self.runs.get(key)
            if model is AgentEventModel:
                return self.events.get(key)
            return None

    class FakeSessionFactory:
        def __init__(self) -> None:
            self.db = FakeDb()

        def __call__(self) -> FakeDb:
            return self.db

    repository = AgentRepository(FakeSessionFactory())

    await repository.create_session("ana_001", {"raw_input": "压力波动"})
    await repository.create_run("ana_001", "run_001")
    await repository.complete_run("ana_001", "run_001", run_status)

    session = await repository.get_session("ana_001")
    assert session is not None
    assert session["status"] == expected_session_status


@pytest.mark.asyncio
async def test_repository_delete_user_memory_returns_memory_without_lazy_timestamp_load() -> None:
    memory = UserMemoryModel(
        id="mem_001",
        user_id="system",
        memory_type="preference",
        preference_key="answer.style",
        content="回答先给结论",
        status="active",
        risk_level="low",
        created_at=datetime(2026, 5, 3, 10, 0, tzinfo=UTC),
        updated_at=datetime(2026, 5, 3, 10, 0, tzinfo=UTC),
    )

    class FakeDb:
        async def __aenter__(self) -> "FakeDb":
            return self

        async def __aexit__(self, exc_type, exc, tb) -> None:
            return None

        async def commit(self) -> None:
            return None

        async def get(self, model, key):
            if model is UserMemoryModel and key == memory.id:
                return memory
            return None

    class FakeSessionFactory:
        def __call__(self) -> FakeDb:
            return FakeDb()

    repository = AgentRepository(FakeSessionFactory())

    result = await repository.delete_user_memory("system", memory.id)

    assert result is not None
    assert result["status"] == "deleted"
    assert isinstance(memory.updated_at, datetime)


@pytest.mark.asyncio
async def test_repository_set_memory_candidate_status_returns_candidate_without_lazy_timestamp_load() -> None:
    candidate = MemoryCandidateModel(
        id="cand_001",
        session_id="ana_001",
        run_id="run_001",
        user_id="system",
        memory_type="preference",
        candidate_type="preference",
        preference_key="answer.style",
        title="回答风格",
        content="回答先给结论",
        status="pending",
        created_at=datetime(2026, 5, 3, 10, 0, tzinfo=UTC),
    )

    class FakeDb:
        async def __aenter__(self) -> "FakeDb":
            return self

        async def __aexit__(self, exc_type, exc, tb) -> None:
            return None

        async def commit(self) -> None:
            return None

        async def get(self, model, key):
            if model is MemoryCandidateModel and key == candidate.id:
                return candidate
            return None

    class FakeSessionFactory:
        def __call__(self) -> FakeDb:
            return FakeDb()

    repository = AgentRepository(FakeSessionFactory())

    result = await repository.set_memory_candidate_status(
        "system",
        candidate.id,
        "accepted",
        "system",
    )

    assert result is not None
    assert result["status"] == "accepted"
    assert isinstance(candidate.reviewed_at, datetime)
