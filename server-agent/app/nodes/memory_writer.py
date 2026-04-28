from app.agent.state import AgentState, MemoryCandidateState


async def memory_writer_node(state: AgentState) -> AgentState:
    state.run.status = "awaiting_user"
    state.memory_candidates = [
        MemoryCandidateState(
            summary="User confirmation is required before writing long-term memory.",
            confidence=0.5,
        )
    ]
    return state
