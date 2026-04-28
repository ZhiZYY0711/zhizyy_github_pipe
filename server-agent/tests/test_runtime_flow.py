import pytest

from app.runtime.orchestrator import AgentOrchestrator


@pytest.mark.asyncio
async def test_runtime_flow_emits_phase1_events() -> None:
    orchestrator = AgentOrchestrator.fake_success()

    events = [
        event
        async for event in orchestrator.run(
            session_id="ana_001",
            run_id="run_001",
            raw_input="A1 cathodic protection station fluctuated in the last 24 hours",
            permissions={"monitoring:read"},
        )
    ]

    assert [event.type.value for event in events] == [
        "run_started",
        "understanding_completed",
        "context_completed",
        "plan_created",
        "tool_started",
        "tool_completed",
        "retrieval_completed",
        "reasoning_completed",
        "recommendation_generated",
        "awaiting_user",
    ]
    assert [event.seq for event in events] == list(range(1, len(events) + 1))
    tool_event = next(event for event in events if event.type.value == "tool_completed")
    assert tool_event.payload["displayHint"] == "query_monitoring_trend"
    assert tool_event.payload["facts"][0]["label"] == "工具结果"
    retrieval_event = next(event for event in events if event.type.value == "retrieval_completed")
    assert retrieval_event.payload["documents"][0]["citation"] == "doc:doc_001#chunk_01"
