from typing import Any

from app.agent.events import AgentEventType
from app.agent.state import RecommendationState
from app.llm.client import LlmStreamChunk
from app.react.prompts import build_action_prompt
from app.react.runtime import ReactRuntime
from app.react.schema import ReactAction
from app.tools.registry import ToolDefinition, ToolRegistry


class ScriptedLlm:
    def __init__(self, actions: list[ReactAction]) -> None:
        self.actions = actions
        self.calls: list[dict[str, str]] = []

    async def complete_json(self, system_prompt: str, user_prompt: str, schema: type) -> Any:
        self.calls.append({"system": system_prompt, "user": user_prompt})
        if schema is ReactAction:
            return self.actions.pop(0)
        if schema is RecommendationState:
            return RecommendationState(
                summary="压力波动需要现场复核",
                risk_level="medium",
                judgment="监测趋势和样例知识均提示需要复核压力波动来源。",
                recommended_actions=["复核压力传感器", "检查最近一小时压力曲线"],
                missing_information=[],
                human_confirmation_required=True,
            )
        raise AssertionError(f"Unexpected schema: {schema}")


class StreamingScriptedLlm:
    def __init__(self, streams: list[list[LlmStreamChunk]]) -> None:
        self.streams = streams

    async def complete_json(self, system_prompt: str, user_prompt: str, schema: type) -> Any:
        raise AssertionError("streaming ReAct runtime should not use complete_json")

    async def stream_chat(
        self,
        system_prompt: str,
        user_prompt: str,
        response_format: dict[str, Any] | None = None,
    ):
        for chunk in self.streams.pop(0):
            yield chunk


async def test_react_runtime_allows_llm_to_choose_search_tool_and_final_answer() -> None:
    async def query_monitoring(payload: dict[str, Any]) -> dict[str, Any]:
        return {
            "summary": "压力最近一小时波动明显。",
            "facts": [{"metric": "pressure", "value": 1.8}],
            "confidence": 0.9,
        }

    async def retriever(query: str) -> list[dict[str, Any]]:
        return [
            {
                "doc_id": "doc_pressure_sop",
                "chunk_id": "main",
                "title": "压力异常处置 SOP",
                "summary": f"命中查询: {query}",
                "source": "domain_knowledge",
                "score": 0.88,
                "citation": "doc:doc_pressure_sop#main",
            }
        ]

    runtime = ReactRuntime(
        tool_registry=ToolRegistry(
            [
                ToolDefinition(
                    "query_monitoring_trend",
                    "查询监测趋势",
                    "monitoring:read",
                    8000,
                    True,
                    query_monitoring,
                )
            ]
        ),
        llm_client=ScriptedLlm(
            [
                ReactAction(
                    thought_summary="先检索压力异常处置知识。",
                    action="knowledge_search",
                    search_query="压力异常处置",
                ),
                ReactAction(
                    thought_summary="再读取监测趋势。",
                    action="tool_call",
                    tool_name="query_monitoring_trend",
                    tool_input={"raw_input": "压力波动"},
                ),
                ReactAction(
                    thought_summary="证据已经足够，输出结论。",
                    action="final_answer",
                ),
            ]
        ),
        retriever=retriever,
    )

    events = [
        event
        async for event in runtime.run(
            session_id="ana_1",
            run_id="run_1",
            raw_input="3 号管段压力波动",
            permissions={"monitoring:read", "knowledge:read"},
        )
    ]

    event_types = [event.type for event in events]
    assert AgentEventType.LLM_STEP_STARTED in event_types
    assert AgentEventType.ACTION_SELECTED in event_types
    assert AgentEventType.KNOWLEDGE_SEARCH_COMPLETED in event_types
    assert AgentEventType.TOOL_COMPLETED in event_types
    assert AgentEventType.RECOMMENDATION_GENERATED in event_types
    assert event_types[-1] == AgentEventType.RUN_COMPLETED

    recommendation = next(event for event in events if event.type == AgentEventType.RECOMMENDATION_GENERATED)
    assert recommendation.payload["summary"] == "压力波动需要现场复核"
    assert "query_monitoring_trend" in recommendation.payload["tool_calls"]


async def test_react_runtime_injects_summary_memory_into_prompt() -> None:
    llm = ScriptedLlm([ReactAction(thought_summary="直接回答。", action="final_answer")])
    runtime = ReactRuntime(
        tool_registry=ToolRegistry([]),
        llm_client=llm,
        retriever=lambda query: [],
        summary_memory=["用户偏好：所有研判结论使用中文并给出操作项。"],
    )

    _ = [
        event
        async for event in runtime.run(
            session_id="ana_2",
            run_id="run_2",
            raw_input="给出压力波动建议",
            permissions=set(),
        )
    ]

    assert "用户偏好" in llm.calls[0]["user"]


def test_action_prompt_includes_tool_catalog_metadata_for_llm_choice() -> None:
    prompt = build_action_prompt(
        raw_input="获取山东省过去一年平均温度",
        step=1,
        available_tools=[
            ToolDefinition(
                "query_monitoring_aggregate",
                "按范围聚合监测指标",
                "monitoring:read",
                8000,
                False,
                lambda payload: None,
                category="monitoring",
                when_to_use="需要按区域、管线、管段、设备和时间范围聚合指标时使用。",
                input_schema={
                    "type": "object",
                    "properties": {
                        "scope_type": {"type": "string"},
                        "scope_id": {"type": "string"},
                        "metric": {"type": "string"},
                    },
                },
            )
        ],
        observations=[],
        retrieved_knowledge=[],
        summary_memory=[],
        limits={"max_steps": 3},
    )

    assert "query_monitoring_aggregate" in prompt
    assert "when_to_use" in prompt
    assert "input_schema" in prompt
    assert "scope_type" in prompt


async def test_react_runtime_streams_thinking_before_model_selected_action() -> None:
    async def query_monitoring(payload: dict[str, Any]) -> dict[str, Any]:
        return {"summary": "压力最近一小时波动明显。"}

    runtime = ReactRuntime(
        tool_registry=ToolRegistry(
            [
                ToolDefinition(
                    "query_monitoring_trend",
                    "查询监测趋势",
                    "monitoring:read",
                    8000,
                    True,
                    query_monitoring,
                )
            ]
        ),
        llm_client=StreamingScriptedLlm(
            [
                [
                    LlmStreamChunk(channel="reasoning_content", delta="先看实时趋势，"),
                    LlmStreamChunk(channel="reasoning_content", delta="再决定是否输出。"),
                    LlmStreamChunk(
                        channel="content",
                        delta=(
                            '{"thought_summary":"需要调用监测趋势工具。",'
                            '"action":"tool_call","tool_name":"query_monitoring_trend",'
                            '"tool_input":{"raw_input":"压力波动"}}'
                        ),
                    ),
                ],
                [
                    LlmStreamChunk(channel="reasoning_content", delta="证据已足够，"),
                    LlmStreamChunk(channel="content", delta='{"thought_summary":"输出结论。","action":"final_answer"}'),
                ],
                [
                    LlmStreamChunk(channel="content", delta='{"summary":"压力波动需要复核",'),
                    LlmStreamChunk(
                        channel="content",
                        delta=(
                            '"risk_level":"medium","judgment":"工具证据提示压力波动。",'
                            '"recommended_actions":["复核压力传感器"],"missing_information":[],'
                            '"human_confirmation_required":true}'
                        ),
                    ),
                ],
            ]
        ),
        retriever=lambda query: [],
    )

    events = [
        event
        async for event in runtime.run(
            session_id="ana_stream",
            run_id="run_stream",
            raw_input="压力波动",
            permissions={"monitoring:read"},
        )
    ]

    event_types = [event.type for event in events]
    first_thinking_delta = event_types.index(AgentEventType.LLM_THINKING_DELTA)
    first_action = event_types.index(AgentEventType.ACTION_SELECTED)
    assert first_thinking_delta < first_action
    assert any(
        event.type == AgentEventType.LLM_THINKING_COMPLETED
        and event.payload["content"] == "先看实时趋势，再决定是否输出。"
        for event in events
    )
    assert any(event.type == AgentEventType.TOOL_COMPLETED for event in events)
    assert any(event.type == AgentEventType.FINAL_ANSWER_DELTA for event in events)
    assert event_types[-1] == AgentEventType.RUN_COMPLETED


async def test_react_runtime_accepts_markdown_json_fence_in_streamed_final_answer() -> None:
    runtime = ReactRuntime(
        tool_registry=ToolRegistry([]),
        llm_client=StreamingScriptedLlm(
            [
                [
                    LlmStreamChunk(
                        channel="content",
                        delta='{"thought_summary":"证据足够，输出结论。","action":"final_answer"}',
                    ),
                ],
                [
                    LlmStreamChunk(channel="content", delta="```json\n"),
                    LlmStreamChunk(
                        channel="content",
                        delta=(
                            '{"summary":"阀室温度异常需要复核",'
                            '"risk_level":"medium","judgment":"温度异常可能来自通风、伴热或传感器漂移。",'
                            '"recommended_actions":["复核温度传感器"],"missing_information":[],'
                            '"human_confirmation_required":true}'
                        ),
                    ),
                    LlmStreamChunk(channel="content", delta="\n```"),
                ],
            ]
        ),
        retriever=lambda query: [],
    )

    events = [
        event
        async for event in runtime.run(
            session_id="ana_fenced",
            run_id="run_fenced",
            raw_input="今日巡检发现阀室温度异常升高",
            permissions=set(),
        )
    ]

    recommendation = next(event for event in events if event.type == AgentEventType.RECOMMENDATION_GENERATED)
    assert recommendation.payload["summary"] == "阀室温度异常需要复核"
    assert [event.type for event in events][-1] == AgentEventType.RUN_COMPLETED


async def test_react_runtime_accepts_markdown_json_fence_in_streamed_action() -> None:
    runtime = ReactRuntime(
        tool_registry=ToolRegistry([]),
        llm_client=StreamingScriptedLlm(
            [
                [
                    LlmStreamChunk(channel="content", delta="```json\n"),
                    LlmStreamChunk(
                        channel="content",
                        delta='{"thought_summary":"证据足够，输出结论。","action":"final_answer"}',
                    ),
                    LlmStreamChunk(channel="content", delta="\n```"),
                ],
                [
                    LlmStreamChunk(
                        channel="content",
                        delta=(
                            '{"summary":"压力波动需要复核",'
                            '"risk_level":"medium","judgment":"压力趋势异常需要现场确认。",'
                            '"recommended_actions":["复核压力传感器"],"missing_information":[],'
                            '"human_confirmation_required":true}'
                        ),
                    ),
                ],
            ]
        ),
        retriever=lambda query: [],
    )

    events = [
        event
        async for event in runtime.run(
            session_id="ana_action_fenced",
            run_id="run_action_fenced",
            raw_input="压力波动",
            permissions=set(),
        )
    ]

    assert any(event.type == AgentEventType.ACTION_SELECTED for event in events)
    assert [event.type for event in events][-1] == AgentEventType.RUN_COMPLETED
