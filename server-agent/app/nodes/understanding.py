from app.agent.state import AgentState


async def understanding_node(state: AgentState) -> AgentState:
    state.run.status = "understanding"
    state.incident.type = "abnormality_analysis"
    state.incident.signals = [state.user_input]
    return state
