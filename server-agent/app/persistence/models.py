from datetime import datetime
from typing import Any

from sqlalchemy import BigInteger, CheckConstraint, DateTime, ForeignKey, Numeric, String, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.sql import func


class Base(DeclarativeBase):
    pass


class AnalysisSessionModel(Base):
    __tablename__ = "analysis_session"
    __table_args__ = (
        CheckConstraint(
            "status IN ('created', 'running', 'awaiting_user', 'completed', 'failed', 'cancelled', 'archived')",
            name="ck_analysis_session_status",
        ),
    )

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(200))
    status: Mapped[str] = mapped_column(String(32), default="created", nullable=False)
    incident_type: Mapped[str | None] = mapped_column(String(64))
    severity: Mapped[str | None] = mapped_column(String(32))
    raw_input: Mapped[str | None] = mapped_column(Text)
    source_type: Mapped[str] = mapped_column(String(32), default="manual")
    source_id: Mapped[str | None] = mapped_column(String(64))
    object_type: Mapped[str | None] = mapped_column(String(64))
    object_id: Mapped[str | None] = mapped_column(String(64))
    object_name: Mapped[str | None] = mapped_column(String(200))
    user_id: Mapped[str] = mapped_column(String(64), default="system")
    org_id: Mapped[str | None] = mapped_column(String(64))
    summary: Mapped[str | None] = mapped_column(Text)
    current_run_id: Mapped[str | None] = mapped_column(String(64))
    pinned: Mapped[bool] = mapped_column(default=False, nullable=False)
    archived_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AgentRunModel(Base):
    __tablename__ = "agent_run"
    __table_args__ = (
        CheckConstraint(
            "status IN ('created', 'running', 'understanding', 'context_building', 'planning', 'retrieving', 'tool_running', 'reasoning', 'awaiting_user', 'completed', 'failed', 'cancelled')",
            name="ck_agent_run_status",
        ),
    )

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    session_id: Mapped[str] = mapped_column(
        String(64), ForeignKey("analysis_session.id", ondelete="CASCADE")
    )
    triggering_message_id: Mapped[str | None] = mapped_column(String(64))
    input_text: Mapped[str | None] = mapped_column(Text)
    status: Mapped[str] = mapped_column(String(32), default="created", nullable=False)
    model_provider: Mapped[str | None] = mapped_column(String(64))
    model_name: Mapped[str | None] = mapped_column(String(128))
    model_tier: Mapped[str | None] = mapped_column(String(32))
    graph_version: Mapped[str | None] = mapped_column(String(64))
    prompt_version: Mapped[str | None] = mapped_column(String(64))
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    duration_ms: Mapped[int | None] = mapped_column(BigInteger)
    error_code: Mapped[str | None] = mapped_column(String(64))
    error_message: Mapped[str | None] = mapped_column(Text)
    state_snapshot: Mapped[dict[str, Any] | None] = mapped_column(JSONB)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class AgentMessageModel(Base):
    __tablename__ = "agent_message"
    __table_args__ = (
        CheckConstraint(
            "role IN ('user', 'assistant', 'system')",
            name="ck_agent_message_role",
        ),
    )

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    session_id: Mapped[str] = mapped_column(
        String(64), ForeignKey("analysis_session.id", ondelete="CASCADE")
    )
    role: Mapped[str] = mapped_column(String(32), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    message_type: Mapped[str] = mapped_column(String(32), default="text", nullable=False)
    triggered_run_id: Mapped[str | None] = mapped_column(String(64))
    metadata_json: Mapped[dict[str, Any] | None] = mapped_column("metadata", JSONB)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class AgentShareModel(Base):
    __tablename__ = "agent_share"
    __table_args__ = (
        CheckConstraint(
            "share_type IN ('link', 'md')",
            name="ck_agent_share_type",
        ),
    )

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    session_id: Mapped[str] = mapped_column(
        String(64), ForeignKey("analysis_session.id", ondelete="CASCADE")
    )
    created_by: Mapped[str] = mapped_column(String(64), default="system", nullable=False)
    share_type: Mapped[str] = mapped_column(String(16), nullable=False)
    title: Mapped[str] = mapped_column(String(200))
    snapshot: Mapped[dict[str, Any]] = mapped_column(JSONB)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AgentRunSummaryModel(Base):
    __tablename__ = "agent_run_summary"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    session_id: Mapped[str] = mapped_column(
        String(64), ForeignKey("analysis_session.id", ondelete="CASCADE")
    )
    run_id: Mapped[str] = mapped_column(String(64), ForeignKey("agent_run.id", ondelete="CASCADE"))
    triggering_message_id: Mapped[str | None] = mapped_column(String(64))
    final_answer: Mapped[str | None] = mapped_column(Text)
    risk_level: Mapped[str | None] = mapped_column(String(32))
    judgment: Mapped[str | None] = mapped_column(Text)
    recommended_actions: Mapped[list[Any] | None] = mapped_column(JSONB)
    key_evidence: Mapped[list[Any] | None] = mapped_column(JSONB)
    open_questions: Mapped[list[Any] | None] = mapped_column(JSONB)
    tool_summary: Mapped[list[Any] | None] = mapped_column(JSONB)
    knowledge_summary: Mapped[list[Any] | None] = mapped_column(JSONB)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class MemoryCandidateModel(Base):
    __tablename__ = "memory_candidate"
    __table_args__ = (
        CheckConstraint(
            "status IN ('pending', 'accepted', 'rejected', 'expired')",
            name="ck_memory_candidate_status",
        ),
        CheckConstraint(
            "confidence IS NULL OR confidence >= 0 AND confidence <= 1",
            name="ck_memory_candidate_confidence",
        ),
    )

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    session_id: Mapped[str] = mapped_column(
        String(64), ForeignKey("analysis_session.id", ondelete="CASCADE")
    )
    run_id: Mapped[str] = mapped_column(String(64), ForeignKey("agent_run.id", ondelete="CASCADE"))
    user_id: Mapped[str] = mapped_column(String(64), default="system", nullable=False)
    memory_type: Mapped[str] = mapped_column(String(64), nullable=False)
    candidate_type: Mapped[str | None] = mapped_column(String(64))
    preference_key: Mapped[str | None] = mapped_column(String(128))
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    risk_level: Mapped[str | None] = mapped_column(String(16))
    proposed_action: Mapped[str | None] = mapped_column(String(32))
    source_text: Mapped[str | None] = mapped_column(Text)
    reason: Mapped[str | None] = mapped_column(Text)
    evidence_ids: Mapped[list[Any] | None] = mapped_column(JSONB)
    confidence: Mapped[float | None] = mapped_column(Numeric(5, 4))
    status: Mapped[str] = mapped_column(String(32), default="pending", nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    reviewed_by: Mapped[str | None] = mapped_column(String(64))


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


class AgentEventModel(Base):
    __tablename__ = "agent_event"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    session_id: Mapped[str] = mapped_column(
        String(64), ForeignKey("analysis_session.id", ondelete="CASCADE")
    )
    run_id: Mapped[str] = mapped_column(String(64), ForeignKey("agent_run.id", ondelete="CASCADE"))
    seq: Mapped[int] = mapped_column(BigInteger)
    type: Mapped[str] = mapped_column(String(64))
    level: Mapped[str] = mapped_column(String(16), default="info")
    title: Mapped[str | None] = mapped_column(String(200))
    message: Mapped[str | None] = mapped_column(Text)
    payload: Mapped[dict[str, Any] | None] = mapped_column(JSONB)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
