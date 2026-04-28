from app.agent.state import AgentState, ContextState, Incident, PlanState, RecommendationState, RunState, SessionState
from app.nodes.reasoner import reasoner_node


class FakeLlmClient:
    async def complete_json(self, system_prompt: str, user_prompt: str, schema: type[RecommendationState]):
        assert "pipeline integrity analysis agent" in system_prompt
        assert "Retrieved knowledge" in user_prompt
        return schema(
            summary="Likely cathodic protection drift.",
            risk_level="medium",
            judgment="Monitoring and historical evidence point to a medium risk abnormality.",
            recommended_actions=["Confirm reference electrode status"],
            missing_information=["Latest field inspection result"],
            human_confirmation_required=True,
        )


async def test_reasoner_node_accepts_structured_llm_recommendation() -> None:
    state = AgentState(
        session=SessionState(id="ana_001", status="running"),
        run=RunState(id="run_001", status="reasoning"),
        user_input="cp voltage abnormal",
        incident=Incident(type="cathodic_protection_abnormal"),
        context=ContextState(),
        plan=PlanState(),
        status="reasoning",
    )

    next_state = await reasoner_node(state, llm_client=FakeLlmClient())

    assert next_state.run.status == "reasoning"
    assert next_state.recommendation is not None
    assert next_state.recommendation.risk_level == "medium"
    assert next_state.recommendation.human_confirmation_required is True
