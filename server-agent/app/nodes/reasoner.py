from app.agent.state import AgentState, HypothesisState, RecommendationState
from app.llm.client import LlmClient
from app.llm.prompts import REASONER_SYSTEM_PROMPT, build_reasoner_prompt


async def reasoner_node(state: AgentState, llm_client: LlmClient | None = None) -> AgentState:
    state.run.status = "reasoning"
    if llm_client is not None:
        state.recommendation = await llm_client.complete_json(
            system_prompt=REASONER_SYSTEM_PROMPT,
            user_prompt=build_reasoner_prompt(state),
            schema=RecommendationState,
        )
        state.hypotheses = [
            HypothesisState(
                summary=state.recommendation.judgment,
                confidence=0.7,
                evidence_refs=[
                    item.citation for item in state.retrieved_knowledge if item.citation is not None
                ],
            )
        ]
        return state

    state.hypotheses = [
        HypothesisState(
            summary="The abnormality needs confirmation with monitoring trend and asset context.",
            confidence=0.6,
            evidence_refs=[],
        )
    ]
    state.recommendation = RecommendationState(
        summary="Current abnormality needs human confirmation.",
        risk_level="medium",
        judgment="The runtime has gathered preliminary evidence but cannot close the case automatically.",
        recommended_actions=[
            "Review monitoring trend",
            "Check related asset or task context",
            "Confirm whether field inspection is required",
        ],
        missing_information=["Primary business object may need confirmation"],
        human_confirmation_required=True,
    )
    return state
