from collections.abc import AsyncIterator
from typing import Awaitable, Callable

from app.agent.events import AgentEvent
from app.llm import build_llm_client
from app.rag.retriever import build_qdrant_retriever
from app.react.runtime import ReactRuntime
from app.runtime.orchestrator import AgentOrchestrator
from app.tools.registry import ToolRegistry

Retriever = Callable[[str], Awaitable[list[dict]]]


class AgentRuntimeService:
    def __init__(
        self,
        tool_registry: ToolRegistry,
        retriever: Retriever | None = None,
        summary_memory: list[str] | None = None,
    ) -> None:
        llm_client = build_llm_client()
        if llm_client is not None:
            self.orchestrator = ReactRuntime(
                tool_registry=tool_registry,
                llm_client=llm_client,
                retriever=retriever or build_qdrant_retriever(),
                summary_memory=summary_memory or [],
            )
            return

        self.orchestrator = AgentOrchestrator.with_tool_registry(tool_registry)
        if retriever is not None:
            self.orchestrator = self.orchestrator.with_retriever(retriever)

    async def run_analysis(
        self,
        session_id: str,
        run_id: str,
        raw_input: str,
        permissions: set[str],
        conversation_context: dict | None = None,
    ) -> AsyncIterator[AgentEvent]:
        async for event in self.orchestrator.run(
            session_id=session_id,
            run_id=run_id,
            raw_input=_with_conversation_context(raw_input, conversation_context),
            permissions=permissions,
        ):
            yield event


def _with_conversation_context(raw_input: str, conversation_context: dict | None) -> str:
    if not conversation_context:
        return raw_input
    messages = conversation_context.get("messages") or []
    summaries = conversation_context.get("run_summaries") or []
    if not messages and not summaries:
        return raw_input
    context_lines = ["本轮用户输入：", raw_input, "", "会话上下文摘要："]
    for message in messages[-6:]:
        role = message.get("role", "message")
        content = message.get("content", "")
        context_lines.append(f"- {role}: {content}")
    for summary in summaries[-3:]:
        final_answer = summary.get("final_answer") or summary.get("judgment") or ""
        if final_answer:
            context_lines.append(f"- 上轮结论: {final_answer}")
    return "\n".join(context_lines)
