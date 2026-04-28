from app.agent.state import AgentState


async def context_builder_node(state: AgentState) -> AgentState:
    state.run.status = "context_building"
    state.context.pipeline = None
    state.context.pipe_segment = None
    return state
