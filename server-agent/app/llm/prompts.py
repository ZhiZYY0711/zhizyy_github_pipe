from app.agent.state import AgentState

REASONER_SYSTEM_PROMPT = (
    "You are a pipeline integrity analysis agent. Return only JSON matching the requested schema."
)


def build_reasoner_prompt(state: AgentState) -> str:
    evidence = [item.model_dump(mode="json") for item in state.evidence]
    retrieved = [item.model_dump(mode="json") for item in state.retrieved_knowledge]
    return (
        f"User input: {state.user_input}\n"
        f"Incident: {state.incident.model_dump(mode='json')}\n"
        f"Tool evidence: {evidence}\n"
        f"Retrieved knowledge: {retrieved}\n"
        "Generate a concise risk judgment, recommended actions, missing information, "
        "and whether human confirmation is required."
    )
