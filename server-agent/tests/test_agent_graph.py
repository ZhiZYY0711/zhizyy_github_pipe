import pytest

from app.agent.events import AgentEventType
from app.agent.service import AgentRuntimeService
from app.tools.registry import ToolDefinition, ToolRegistry
from app.tools.mock_tools import build_mock_registry


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


@pytest.mark.asyncio
async def test_agent_runtime_emits_plan_tool_and_recommendation_events():
    registry = build_mock_registry()
    permissions = {"monitoring:read", "pipeline:read", "knowledge:read"}
    service = AgentRuntimeService(tool_registry=registry, retriever=fake_retriever)
    events = [
        event
        async for event in service.run_analysis(
            session_id="ana_001",
            run_id="run_001",
            raw_input="CP-04 阴保电位波动超过阈值",
            permissions=permissions,
        )
    ]

    assert [event.seq for event in events] == list(range(1, len(events) + 1))

    available_tools = registry.available_tools(permissions)
    expected_types = [
        AgentEventType.RUN_STARTED.value,
        AgentEventType.UNDERSTANDING_COMPLETED.value,
        AgentEventType.CONTEXT_COMPLETED.value,
        AgentEventType.PLAN_CREATED.value,
    ]
    for _tool in available_tools:
        expected_types.extend([AgentEventType.TOOL_STARTED.value, AgentEventType.TOOL_COMPLETED.value])
    expected_types.extend(
        [
            AgentEventType.RETRIEVAL_COMPLETED.value,
            AgentEventType.REASONING_COMPLETED.value,
            AgentEventType.RECOMMENDATION_GENERATED.value,
            AgentEventType.AWAITING_USER.value,
        ]
    )

    assert [event.type.value for event in events] == expected_types

    tool_completed_event = next(event for event in events if event.type == AgentEventType.TOOL_COMPLETED)
    assert tool_completed_event.payload["tool_name"] == "query_monitoring_trend"
    assert tool_completed_event.payload["summary"]
    assert tool_completed_event.payload["input"] == {"raw_input": "CP-04 阴保电位波动超过阈值"}
    assert isinstance(tool_completed_event.payload["context"], dict)
    assert isinstance(tool_completed_event.payload["evidence"], dict)


@pytest.mark.asyncio
async def test_agent_runtime_emits_tool_and_run_failed_events_on_tool_error():
    async def failing_tool(payload: dict[str, object]) -> dict[str, object]:
        raise RuntimeError("boom")

    registry = ToolRegistry(
        [
            ToolDefinition("failing_tool", "failing", "monitoring:read", 1000, False, failing_tool),
        ]
    )
    service = AgentRuntimeService(tool_registry=registry)

    events = [
        event
        async for event in service.run_analysis(
            session_id="ana_fail",
            run_id="run_fail",
            raw_input="input",
            permissions={"monitoring:read"},
        )
    ]

    assert [event.type.value for event in events] == [
        AgentEventType.RUN_STARTED.value,
        AgentEventType.UNDERSTANDING_COMPLETED.value,
        AgentEventType.CONTEXT_COMPLETED.value,
        AgentEventType.PLAN_CREATED.value,
        AgentEventType.TOOL_STARTED.value,
        AgentEventType.TOOL_FAILED.value,
        AgentEventType.RUN_FAILED.value,
    ]
    assert [event.seq for event in events] == list(range(1, len(events) + 1))
    assert events[-2].payload == {"tool_name": "failing_tool", "error": "boom"}
    assert events[-1].payload == {"status": "failed", "tool_name": "failing_tool"}
