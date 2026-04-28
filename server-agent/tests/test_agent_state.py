import pytest
from pydantic import ValidationError

from app.agent.events import AgentEventType
from app.agent.state import AgentState


def test_agent_state_matches_phase1_contract() -> None:
    state = AgentState.model_validate(
        {
            "session": {"id": "ana_001", "status": "created"},
            "run": {"id": "run_001", "status": "created"},
            "user_input": "阴保电位异常",
            "incident": {"type": "cathodic_protection_abnormal"},
            "context": {},
            "plan": {"steps": []},
            "evidence": [],
            "retrieved_knowledge": [],
            "hypotheses": [],
            "recommendation": None,
            "memory_candidates": [],
            "status": "created",
            "errors": [],
        }
    )

    assert state.session.id == "ana_001"
    assert state.session.status == "created"
    assert state.run.id == "run_001"
    assert state.run.status == "created"
    assert state.user_input == "阴保电位异常"
    assert state.incident.type == "cathodic_protection_abnormal"
    assert state.context.pipeline is None
    assert state.plan.steps == []
    assert state.evidence == []
    assert state.retrieved_knowledge == []
    assert state.hypotheses == []
    assert state.recommendation is None
    assert state.memory_candidates == []
    assert state.status == "created"
    assert state.errors == []


def test_agent_event_types_match_phase1_contract() -> None:
    event_types = {member.value for member in AgentEventType}

    assert {
        "run_started",
        "understanding_completed",
        "context_completed",
        "plan_created",
        "tool_started",
        "tool_completed",
        "tool_failed",
        "retrieval_completed",
        "reasoning_completed",
        "recommendation_generated",
        "awaiting_user",
        "run_completed",
        "run_failed",
        "run_cancel_requested",
        "run_cancelled",
    }.issubset(event_types)

    assert {
        "context_built",
        "llm_step_started",
        "llm_step_completed",
        "action_selected",
        "tool_progress",
        "knowledge_search_started",
        "knowledge_search_completed",
        "memory_search_completed",
        "final_answer_started",
        "final_answer_delta",
        "final_answer_completed",
    }.issubset(event_types)


def test_agent_state_rejects_mismatched_top_level_status() -> None:
    with pytest.raises(ValidationError):
        AgentState.model_validate(
            {
                "session": {"id": "ana_001", "status": "created"},
                "run": {"id": "run_001", "status": "completed"},
                "user_input": "阴保电位异常",
                "status": "created",
            }
        )


def test_agent_state_keeps_statuses_in_sync_on_assignment() -> None:
    state = AgentState.model_validate(
        {
            "session": {"id": "ana_001", "status": "created"},
            "run": {"id": "run_001", "status": "created"},
            "user_input": "阴保电位异常",
        }
    )

    state.run.status = "failed"
    assert state.status == "failed"

    state.status = "completed"
    assert state.run.status == "completed"
