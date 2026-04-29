from collections.abc import AsyncIterator, Awaitable, Callable
from uuid import uuid4

from app.agent.events import AgentEvent, AgentEventType
from app.agent.state import (
    AgentState,
    ContextState,
    Incident,
    PlanState,
    RecommendationState,
    RetrievedKnowledgeItem,
    RunState,
    SessionState,
)
from app.llm import build_llm_client
from app.nodes.context_builder import context_builder_node
from app.nodes.memory_writer import memory_writer_node
from app.nodes.planner import planner_node
from app.nodes.reasoner import reasoner_node
from app.nodes.understanding import understanding_node
from app.rag.retriever import build_qdrant_retriever
from app.runtime.graph import RuntimeGraph, build_phase1_graph
from app.tools.registry import ToolDefinition, ToolRegistry

Retriever = Callable[[str], Awaitable[list[dict]]]


def _display_facts(result: dict) -> list[dict]:
    facts = result.get("facts")
    if isinstance(facts, list) and facts:
        return facts

    evidence = result.get("evidence")
    if isinstance(evidence, dict) and evidence:
        return [
            {
                "label": "工具结果",
                "value": str(evidence.get("title") or result.get("summary") or "已返回"),
                "status": "normal",
            }
        ]

    if result.get("summary"):
        return [{"label": "工具结果", "value": str(result["summary"]), "status": "normal"}]

    return []


class AgentOrchestrator:
    def __init__(
        self,
        graph: RuntimeGraph,
        tool_registry: ToolRegistry,
        retriever: Retriever | None = None,
    ) -> None:
        self._graph = graph
        self._tool_registry = tool_registry
        self._retriever = retriever or build_qdrant_retriever()

    @classmethod
    def with_tool_registry(cls, tool_registry: ToolRegistry) -> "AgentOrchestrator":
        llm_client = build_llm_client()

        async def configured_reasoner(state: AgentState) -> AgentState:
            return await reasoner_node(state, llm_client=llm_client)

        return cls(
            graph=build_phase1_graph(
                understanding_node,
                context_builder_node,
                planner_node,
                configured_reasoner,
                memory_writer_node,
            ),
            tool_registry=tool_registry,
        )

    @classmethod
    def fake_success(cls) -> "AgentOrchestrator":
        async def fake_tool(payload: dict) -> dict:
            return {
                "summary": "test tool completed",
                "input": payload,
                "context": {},
                "evidence": {"title": "test evidence"},
            }

        async def fake_retriever(query: str) -> list[dict]:
            return [
                {
                    "doc_id": "doc_001",
                    "chunk_id": "chunk_01",
                    "summary": query,
                    "source": "domain_knowledge",
                    "citation": "doc:doc_001#chunk_01",
                }
            ]

        return cls.with_tool_registry(
            ToolRegistry(
                [
                    ToolDefinition(
                        "query_monitoring_trend",
                        "Query monitoring trend",
                        "monitoring:read",
                        1000,
                        False,
                        fake_tool,
                    )
                ]
            )
        ).with_retriever(fake_retriever)

    def with_retriever(self, retriever: Retriever) -> "AgentOrchestrator":
        return AgentOrchestrator(self._graph, self._tool_registry, retriever)

    async def run(
        self,
        session_id: str,
        run_id: str,
        raw_input: str,
        permissions: set[str] | None = None,
    ) -> AsyncIterator[AgentEvent]:
        seq = 1
        state = AgentState(
            session=SessionState(id=session_id, status="running"),
            run=RunState(id=run_id, status="created"),
            user_input=raw_input,
            incident=Incident(),
            context=ContextState(),
            plan=PlanState(),
            recommendation=None,
            status="created",
        )

        def event(event_type: AgentEventType, title: str, payload: dict) -> AgentEvent:
            nonlocal seq
            current = AgentEvent(
                id=f"evt_{uuid4().hex}",
                session_id=session_id,
                run_id=run_id,
                seq=seq,
                type=event_type,
                title=title,
                payload=payload,
            )
            seq += 1
            return current

        yield event(AgentEventType.RUN_STARTED, "Start analysis", {"raw_input": raw_input})

        async for state in self._graph.run(state):
            if state.run.status == "understanding":
                yield event(
                    AgentEventType.UNDERSTANDING_COMPLETED,
                    "Understanding completed",
                    {"incident": state.incident.model_dump(mode="json")},
                )
            elif state.run.status == "context_building":
                yield event(
                    AgentEventType.CONTEXT_COMPLETED,
                    "Context completed",
                    {"context": state.context.model_dump(mode="json")},
                )
            elif state.run.status == "planning":
                yield event(
                    AgentEventType.PLAN_CREATED,
                    "Plan created",
                    {"plan": state.plan.model_dump(mode="json")},
                )
                async for tool_event in self._run_tools(event, raw_input, permissions or set()):
                    yield tool_event
                    if tool_event.type == AgentEventType.RUN_FAILED:
                        return
                documents = await self._retriever(raw_input)
                state.retrieved_knowledge = [
                    RetrievedKnowledgeItem.model_validate(document) for document in documents
                ]
                yield event(
                    AgentEventType.RETRIEVAL_COMPLETED,
                    "Retrieval completed",
                    {
                        "source": "domain_knowledge",
                        "documents": [
                            document.model_dump(mode="json") for document in state.retrieved_knowledge
                        ],
                    },
                )
            elif state.run.status == "reasoning":
                yield event(
                    AgentEventType.REASONING_COMPLETED,
                    "Reasoning completed",
                    {"hypotheses": [item.model_dump(mode="json") for item in state.hypotheses]},
                )
                recommendation = state.recommendation or RecommendationState(
                    summary="Analysis needs human confirmation.",
                    risk_level="medium",
                    judgment="Current evidence is not enough for automatic closure.",
                    recommended_actions=[],
                    missing_information=[],
                    human_confirmation_required=True,
                )
                yield event(
                    AgentEventType.RECOMMENDATION_GENERATED,
                    "Recommendation generated",
                    recommendation.model_dump(mode="json"),
                )
            elif state.run.status == "awaiting_user":
                yield event(
                    AgentEventType.AWAITING_USER,
                    "Awaiting user confirmation",
                    {"status": "awaiting_user"},
                )

    async def _run_tools(
        self,
        event_factory: Callable[[AgentEventType, str, dict], AgentEvent],
        raw_input: str,
        permissions: set[str],
    ) -> AsyncIterator[AgentEvent]:
        for tool in self._tool_registry.available_tools(permissions):
            yield event_factory(
                AgentEventType.TOOL_STARTED,
                f"Tool started: {tool.name}",
                {"tool_name": tool.name},
            )
            try:
                result = await self._tool_registry.invoke(tool.name, {"raw_input": raw_input})
            except Exception as exc:
                yield event_factory(
                    AgentEventType.TOOL_FAILED,
                    f"Tool failed: {tool.name}",
                    {"tool_name": tool.name, "error": str(exc)},
                )
                yield event_factory(
                    AgentEventType.RUN_FAILED,
                    "Analysis failed",
                    {"status": "failed", "tool_name": tool.name},
                )
                return

            payload = {
                "tool_name": tool.name,
                "displayHint": result.get("displayHint") or tool.name,
                "summary": result.get("summary"),
                "facts": _display_facts(result),
            }
            for key in ("input", "context", "evidence", "raw", "raw_ref", "confidence", "metadata"):
                if key in result:
                    payload[key] = result[key]
            yield event_factory(AgentEventType.TOOL_COMPLETED, f"Tool completed: {tool.name}", payload)
