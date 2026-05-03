from datetime import datetime, timezone
from enum import StrEnum
from typing import Any, Literal

from pydantic import BaseModel, Field


class AgentEventType(StrEnum):
    RUN_STARTED = "run_started"
    RUN_PREPARATION_COMPLETED = "run_preparation_completed"
    CONTEXT_BUILT = "context_built"
    LLM_STEP_STARTED = "llm_step_started"
    LLM_THINKING_STARTED = "llm_thinking_started"
    LLM_THINKING_DELTA = "llm_thinking_delta"
    LLM_THINKING_COMPLETED = "llm_thinking_completed"
    LLM_STEP_COMPLETED = "llm_step_completed"
    ACTION_SELECTED = "action_selected"
    ACTION_TEXT_DELTA = "action_text_delta"
    UNDERSTANDING_COMPLETED = "understanding_completed"
    CONTEXT_COMPLETED = "context_completed"
    PLAN_CREATED = "plan_created"
    TOOL_STARTED = "tool_started"
    TOOL_PROGRESS = "tool_progress"
    TOOL_COMPLETED = "tool_completed"
    TOOL_FAILED = "tool_failed"
    KNOWLEDGE_SEARCH_STARTED = "knowledge_search_started"
    KNOWLEDGE_SEARCH_COMPLETED = "knowledge_search_completed"
    MEMORY_SEARCH_COMPLETED = "memory_search_completed"
    MEMORY_ACCEPTED = "memory_accepted"
    MEMORY_CANDIDATE_CREATED = "memory_candidate_created"
    MEMORY_RECALLED = "memory_recalled"
    RETRIEVAL_COMPLETED = "retrieval_completed"
    REASONING_COMPLETED = "reasoning_completed"
    FINAL_ANSWER_STARTED = "final_answer_started"
    FINAL_ANSWER_DELTA = "final_answer_delta"
    FINAL_ANSWER_COMPLETED = "final_answer_completed"
    RECOMMENDATION_GENERATED = "recommendation_generated"
    EXPORT_CREATED = "export_created"
    EXPORT_FAILED = "export_failed"
    AWAITING_USER = "awaiting_user"
    RUN_COMPLETED = "run_completed"
    RUN_FAILED = "run_failed"
    RUN_CANCEL_REQUESTED = "run_cancel_requested"
    RUN_CANCELLED = "run_cancelled"


class AgentEvent(BaseModel):
    id: str
    session_id: str
    run_id: str
    seq: int
    type: AgentEventType
    level: Literal["info", "warn", "error"] = "info"
    title: str | None = None
    message: str | None = None
    payload: dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
