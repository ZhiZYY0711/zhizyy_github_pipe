from app.agent.state import AgentState, PlanState, PlanStep


async def planner_node(state: AgentState) -> AgentState:
    state.run.status = "planning"
    state.plan = PlanState(
        steps=[
            PlanStep(id="understanding", name="Understand incident", status="done"),
            PlanStep(id="context", name="Build business context", status="done"),
            PlanStep(id="tools", name="Gather read-only evidence"),
            PlanStep(id="retrieval", name="Retrieve domain knowledge"),
            PlanStep(id="reasoning", name="Generate recommendation"),
        ],
        required_tools=[
            "query_pipe_segment_context",
            "query_monitoring_trend",
            "search_similar_cases",
        ],
        retrieval_targets=["domain_knowledge", "similar_cases"],
        stop_conditions=["missing_primary_object"],
    )
    return state
