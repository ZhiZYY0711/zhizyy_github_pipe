from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, PrivateAttr, model_validator


RunStatus = Literal[
    "created",
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
]

SessionStatus = Literal[
    "created",
    "running",
    "awaiting_user",
    "completed",
    "failed",
    "cancelled",
    "archived",
]


class SessionState(BaseModel):
    model_config = ConfigDict(validate_assignment=True)

    id: str
    status: SessionStatus = "created"


class RunState(BaseModel):
    model_config = ConfigDict(validate_assignment=True)

    _owner: "AgentState | None" = PrivateAttr(default=None)

    id: str
    status: RunStatus = "created"

    def __setattr__(self, name: str, value: Any) -> None:
        super().__setattr__(name, value)
        if name == "status" and self._owner is not None:
            object.__setattr__(self._owner, "status", value)


class IncidentObject(BaseModel):
    type: str
    id: str
    name: str | None = None


class TimeRange(BaseModel):
    mode: str = "recent"
    duration: str | None = "24h"
    start: str | None = None
    end: str | None = None


class Incident(BaseModel):
    type: str | None = None
    signals: list[str] = Field(default_factory=list)
    objects: list[IncidentObject] = Field(default_factory=list)
    time_range: TimeRange = Field(default_factory=TimeRange)
    urgency: str | None = None
    missing_fields: list[str] = Field(default_factory=list)


class PlanStep(BaseModel):
    id: str
    name: str
    status: Literal["pending", "running", "done", "failed", "skipped"] = "pending"


class ContextState(BaseModel):
    pipeline: str | None = None
    pipe_segment: str | None = None
    sensor_refs: list[str] = Field(default_factory=list)
    equipment_refs: list[str] = Field(default_factory=list)
    task_refs: list[str] = Field(default_factory=list)
    alert_refs: list[str] = Field(default_factory=list)


class PlanState(BaseModel):
    steps: list[PlanStep] = Field(default_factory=list)
    required_tools: list[str] = Field(default_factory=list)
    retrieval_targets: list[str] = Field(default_factory=list)
    stop_conditions: list[str] = Field(default_factory=list)


class EvidenceItem(BaseModel):
    id: str | None = None
    tool_name: str | None = None
    summary: str | None = None
    facts: list[dict[str, Any]] = Field(default_factory=list)
    raw_ref: dict[str, Any] | None = None
    confidence: float | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class RetrievedKnowledgeItem(BaseModel):
    doc_id: str | None = None
    chunk_id: str | None = None
    title: str | None = None
    summary: str | None = None
    source: str | None = None
    score: float | None = None
    citation: str | None = None


class HypothesisState(BaseModel):
    summary: str
    confidence: float | None = None
    evidence_refs: list[str] = Field(default_factory=list)
    rationale: str | None = None


class RecommendationState(BaseModel):
    summary: str
    risk_level: Literal["low", "medium", "high", "critical"]
    judgment: str
    recommended_actions: list[str] = Field(default_factory=list)
    missing_information: list[str] = Field(default_factory=list)
    human_confirmation_required: bool = True


class MemoryCandidateState(BaseModel):
    summary: str
    status: Literal["candidate", "confirmed", "rejected"] = "candidate"
    source_refs: list[str] = Field(default_factory=list)
    confidence: float | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class RuntimeErrorState(BaseModel):
    code: str | None = None
    message: str
    stage: str | None = None
    details: dict[str, Any] = Field(default_factory=dict)


class AgentState(BaseModel):
    model_config = ConfigDict(extra="ignore", validate_assignment=True)

    session: SessionState
    run: RunState
    user_input: str
    incident: Incident = Field(default_factory=Incident)
    context: ContextState = Field(default_factory=ContextState)
    plan: PlanState = Field(default_factory=PlanState)
    evidence: list[EvidenceItem] = Field(default_factory=list)
    retrieved_knowledge: list[RetrievedKnowledgeItem] = Field(default_factory=list)
    hypotheses: list[HypothesisState] = Field(default_factory=list)
    recommendation: RecommendationState | None = None
    memory_candidates: list[MemoryCandidateState] = Field(default_factory=list)
    status: RunStatus = "created"
    errors: list[RuntimeErrorState] = Field(default_factory=list)

    @model_validator(mode="before")
    @classmethod
    def sync_top_level_status_with_run_status(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            return data

        run = data.get("run")
        run_status = run.get("status") if isinstance(run, dict) else None
        status = data.get("status")

        if status is None and run_status is not None:
            data = dict(data)
            data["status"] = run_status
            return data

        if status is not None and run_status is not None and status != run_status:
            raise ValueError("AgentState.status must match run.status")

        if status is None and run_status is None:
            data = dict(data)
            data["status"] = "created"
        return data

    def model_post_init(self, __context: Any) -> None:
        self.run._owner = self

    def __setattr__(self, name: str, value: Any) -> None:
        super().__setattr__(name, value)
        if name == "status":
            object.__setattr__(self.run, "status", value)
