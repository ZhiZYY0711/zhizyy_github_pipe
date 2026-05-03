from collections.abc import AsyncIterator, Awaitable, Callable
from typing import Any
from uuid import uuid4

from app.agent.events import AgentEvent, AgentEventType
from app.llm.client import LlmClient, normalize_json_content
from app.react.prompts import REACT_SYSTEM_PROMPT, build_action_prompt, build_final_prompt
from app.react.schema import ReactAction
from app.tools.registry import ToolRegistry

Retriever = Callable[[str], Awaitable[list[dict[str, Any]]]]


class ReactRuntime:
    def __init__(
        self,
        tool_registry: ToolRegistry,
        llm_client: LlmClient,
        retriever: Retriever,
        summary_memory: list[str] | None = None,
        preference_memory: list[str] | None = None,
        max_steps: int = 8,
        max_tool_calls: int = 5,
        max_knowledge_searches: int = 3,
        max_actions_per_step: int = 3,
    ) -> None:
        self._tool_registry = tool_registry
        self._llm_client = llm_client
        self._retriever = retriever
        self._summary_memory = summary_memory or []
        self._preference_memory = preference_memory or []
        self._max_steps = max_steps
        self._max_tool_calls = max_tool_calls
        self._max_knowledge_searches = max_knowledge_searches
        self._max_actions_per_step = max_actions_per_step

    async def run(
        self,
        session_id: str,
        run_id: str,
        raw_input: str,
        permissions: set[str],
    ) -> AsyncIterator[AgentEvent]:
        seq = 1
        observations: list[dict[str, Any]] = []
        retrieved_knowledge: list[dict[str, Any]] = []
        tool_calls: list[str] = []
        missing_information: list[str] = []
        tool_call_count = 0
        knowledge_search_count = 0

        def event(
            event_type: AgentEventType,
            title: str,
            payload: dict[str, Any],
            level: str = "info",
        ) -> AgentEvent:
            nonlocal seq
            item = AgentEvent(
                id=f"evt_{uuid4().hex}",
                session_id=session_id,
                run_id=run_id,
                seq=seq,
                type=event_type,
                level=level,  # type: ignore[arg-type]
                title=title,
                payload=payload,
            )
            seq += 1
            return item

        available_tools = self._tool_registry.available_tools(permissions)
        limits = {
            "max_steps": self._max_steps,
            "max_tool_calls": self._max_tool_calls,
            "max_knowledge_searches": self._max_knowledge_searches,
        }

        yield event(AgentEventType.RUN_STARTED, "Run started", {"raw_input": raw_input})
        yield event(
            AgentEventType.CONTEXT_BUILT,
            "Context built",
            {
                "available_tools": [tool.name for tool in available_tools],
                "summary_memory_count": len(self._summary_memory),
                "preference_memory_count": len(self._preference_memory),
            },
        )
        if self._summary_memory:
            yield event(
                AgentEventType.MEMORY_SEARCH_COMPLETED,
                "Summary memory loaded",
                {"items": self._summary_memory},
            )

        final_requested = False
        for step in range(1, self._max_steps + 1):
            yield event(AgentEventType.LLM_STEP_STARTED, f"LLM step {step} started", {"step": step})
            action_prompt = build_action_prompt(
                raw_input=raw_input,
                step=step,
                available_tools=available_tools,
                observations=_compact_observations(observations),
                retrieved_knowledge=_compact_knowledge(retrieved_knowledge),
                summary_memory=self._summary_memory,
                preference_memory=self._preference_memory,
                limits=limits,
            )
            if hasattr(self._llm_client, "stream_chat"):
                action_text = ""
                thinking_text = ""
                thinking_started = False
                async for chunk in self._llm_client.stream_chat(
                    system_prompt=REACT_SYSTEM_PROMPT,
                    user_prompt=action_prompt,
                    response_format={"type": "json_object"},
                ):
                    if chunk.channel == "reasoning_content":
                        if not thinking_started:
                            thinking_started = True
                            yield event(
                                AgentEventType.LLM_THINKING_STARTED,
                                f"LLM step {step} thinking started",
                                {"step": step, "channel": chunk.channel},
                            )
                        thinking_text += chunk.delta
                        yield event(
                            AgentEventType.LLM_THINKING_DELTA,
                            f"LLM step {step} thinking delta",
                            {"step": step, "delta": chunk.delta, "channel": chunk.channel},
                        )
                    elif chunk.channel == "content":
                        action_text += chunk.delta
                        yield event(
                            AgentEventType.ACTION_TEXT_DELTA,
                            f"LLM step {step} action delta",
                            {"step": step, "delta": chunk.delta, "channel": chunk.channel},
                        )
                if thinking_started:
                    yield event(
                        AgentEventType.LLM_THINKING_COMPLETED,
                        f"LLM step {step} thinking completed",
                        {"step": step, "content": thinking_text, "channel": "reasoning_content"},
                    )
                action = ReactAction.model_validate_json(normalize_json_content(action_text))
            else:
                action = await self._llm_client.complete_json(
                    system_prompt=REACT_SYSTEM_PROMPT,
                    user_prompt=action_prompt,
                    schema=ReactAction,
                )
            yield event(
                AgentEventType.ACTION_SELECTED,
                f"Action selected: {action.action or 'batch'}",
                action.model_dump(mode="json"),
            )

            step_action_summaries: list[str] = []
            step_actions = action.step_actions()[: self._max_actions_per_step]
            for action_index, sub_action in enumerate(step_actions, start=1):
                step_action_summaries.append(sub_action.action)
                action_meta = {
                    "step": step,
                    "action_index": action_index,
                    "action_count": len(step_actions),
                    "action": sub_action.action,
                }
                if sub_action.action == "final_answer":
                    final_requested = True
                    break

                if sub_action.action == "ask_user":
                    missing_information.extend(sub_action.missing_information)
                    final_requested = True
                    yield event(
                        AgentEventType.AWAITING_USER,
                        "Need more information",
                        {**action_meta, "missing_information": sub_action.missing_information},
                        level="warn",
                    )
                    break

                if sub_action.action == "memory_search":
                    yield event(
                        AgentEventType.MEMORY_SEARCH_COMPLETED,
                        "Summary memory searched",
                        {**action_meta, "query": sub_action.search_query, "items": self._summary_memory},
                    )
                elif sub_action.action == "knowledge_search":
                    if knowledge_search_count >= self._max_knowledge_searches:
                        missing_information.append("知识检索次数已达上限")
                    else:
                        knowledge_search_count += 1
                        yield event(
                            AgentEventType.KNOWLEDGE_SEARCH_STARTED,
                            "Knowledge search started",
                            {**action_meta, "query": sub_action.search_query},
                        )
                        documents = await self._retriever(sub_action.search_query or raw_input)
                        retrieved_knowledge.extend(documents)
                        yield event(
                            AgentEventType.KNOWLEDGE_SEARCH_COMPLETED,
                            "Knowledge search completed",
                            {**action_meta, "query": sub_action.search_query, "documents": documents},
                        )
                elif sub_action.action == "tool_call":
                    if tool_call_count >= self._max_tool_calls:
                        missing_information.append("工具调用次数已达上限")
                    else:
                        tool_call_count += 1
                        tool_name = sub_action.tool_name or ""
                        tool_calls.append(tool_name)
                        yield event(
                            AgentEventType.TOOL_STARTED,
                            f"Tool started: {tool_name}",
                            {**action_meta, "tool_name": tool_name, "input": sub_action.tool_input},
                        )
                        try:
                            result = await self._tool_registry.invoke(tool_name, sub_action.tool_input)
                        except Exception as exc:
                            yield event(
                                AgentEventType.TOOL_FAILED,
                                f"Tool failed: {tool_name}",
                                {**action_meta, "tool_name": tool_name, "error": str(exc)},
                                level="error",
                            )
                        else:
                            observation = {**action_meta, "tool_name": tool_name, **result}
                            observations.append(observation)
                            yield event(
                                AgentEventType.TOOL_COMPLETED,
                                f"Tool completed: {tool_name}",
                                observation,
                            )

            yield event(
                AgentEventType.LLM_STEP_COMPLETED,
                f"LLM step {step} completed",
                {"step": step, "action": action.action or "batch", "actions": step_action_summaries},
            )
            if final_requested:
                break

        yield event(AgentEventType.FINAL_ANSWER_STARTED, "Final answer started", {})
        final_prompt = build_final_prompt(
            raw_input=raw_input,
            observations=_compact_observations(observations),
            retrieved_knowledge=_compact_knowledge(retrieved_knowledge),
            tool_calls=tool_calls,
            missing_information=missing_information,
        )
        if hasattr(self._llm_client, "stream_chat"):
            final_answer = ""
            async for chunk in self._llm_client.stream_chat(
                system_prompt=REACT_SYSTEM_PROMPT,
                user_prompt=final_prompt,
            ):
                if chunk.channel == "content":
                    final_answer += chunk.delta
                    yield event(
                        AgentEventType.FINAL_ANSWER_DELTA,
                        "Final answer delta",
                        {"delta": chunk.delta, "channel": chunk.channel},
                    )
        elif hasattr(self._llm_client, "complete_text"):
            final_answer = await self._llm_client.complete_text(
                system_prompt=REACT_SYSTEM_PROMPT,
                user_prompt=final_prompt,
            )
            yield event(
                AgentEventType.FINAL_ANSWER_DELTA,
                "Final answer delta",
                {"delta": final_answer, "channel": "content"},
            )
        else:
            final_answer = "## 查询结果\n\n最终回答生成器暂不可用。"
        payload = {
            "summary": _markdown_summary(final_answer),
            "judgment": final_answer,
            "final_answer": final_answer,
            "answer_format": "markdown",
            "recommended_actions": [],
            "missing_information": missing_information,
        }
        payload["tool_calls"] = tool_calls
        payload["evidence_refs"] = [
            item.get("citation")
            for item in retrieved_knowledge
            if isinstance(item, dict) and item.get("citation")
        ]
        payload["final_requested"] = final_requested
        yield event(AgentEventType.RECOMMENDATION_GENERATED, "Recommendation generated", payload)
        yield event(AgentEventType.FINAL_ANSWER_COMPLETED, "Final answer completed", payload)
        yield event(AgentEventType.RUN_COMPLETED, "Run completed", {"status": "completed"})


def _compact_observations(observations: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [_compact_observation(observation) for observation in observations]


def _markdown_summary(markdown: str) -> str:
    for line in markdown.splitlines():
        text = line.strip().lstrip("#").strip()
        if text:
            return text[:120]
    return "已生成 Markdown 回答"


def _compact_observation(observation: dict[str, Any]) -> dict[str, Any]:
    compact: dict[str, Any] = {}
    for key in ("tool_name", "displayHint", "summary", "confidence"):
        if key in observation:
            compact[key] = observation[key]

    facts = observation.get("facts")
    if isinstance(facts, list):
        compact["facts"] = [_compact_fact(fact) for fact in facts[:6] if isinstance(fact, dict)]

    context = observation.get("context")
    if isinstance(context, dict):
        compact["context"] = _compact_mapping(context)

    evidence = observation.get("evidence")
    if isinstance(evidence, dict):
        compact["evidence"] = _compact_mapping(evidence)

    raw = observation.get("raw")
    if isinstance(raw, dict):
        compact["raw_summary"] = _summarize_raw_payload(raw)

    return compact


def _compact_fact(fact: dict[str, Any]) -> dict[str, Any]:
    return {
        key: fact[key]
        for key in ("label", "value", "status", "metric", "window")
        if key in fact and _is_prompt_scalar(fact[key])
    }


def _compact_mapping(payload: dict[str, Any], limit: int = 8) -> dict[str, Any]:
    compact: dict[str, Any] = {}
    for key, value in payload.items():
        if len(compact) >= limit:
            break
        if _is_prompt_scalar(value):
            compact[key] = value
    return compact


def _summarize_raw_payload(raw: dict[str, Any]) -> dict[str, Any]:
    summary = _compact_mapping(raw)
    for key in ("records", "points", "items"):
        value = raw.get(key)
        if isinstance(value, list):
            summary[f"{key}_count"] = len(value)
    return summary


def _compact_knowledge(documents: list[dict[str, Any]]) -> list[dict[str, Any]]:
    compact_documents = []
    for document in documents[:5]:
        compact_documents.append(
            {
                key: document[key]
                for key in ("title", "summary", "citation", "source", "score")
                if key in document and _is_prompt_scalar(document[key])
            }
        )
    return compact_documents


def _is_prompt_scalar(value: Any) -> bool:
    return value is None or isinstance(value, str | int | float | bool)
