import asyncio
from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field
from typing import Any


ToolHandler = Callable[[dict[str, Any]], Awaitable[dict[str, Any]]]


@dataclass(frozen=True)
class ToolDefinition:
    name: str
    description: str
    permission: str
    timeout_ms: int
    requires_confirmation: bool
    handler: ToolHandler
    category: str = "general"
    when_to_use: str = ""
    input_schema: dict[str, Any] = field(default_factory=dict)
    output_schema: dict[str, Any] = field(default_factory=dict)
    examples: list[str] = field(default_factory=list)


class ToolRegistry:
    def __init__(self, tools: list[ToolDefinition]) -> None:
        self._tools = {}
        for tool in tools:
            if tool.name in self._tools:
                raise ValueError(f"Duplicate tool name: {tool.name}")
            self._tools[tool.name] = tool

    def available_tools(self, permissions: set[str]) -> list[ToolDefinition]:
        return [tool for tool in self._tools.values() if tool.permission in permissions]

    async def invoke(self, name: str, payload: dict[str, Any]) -> dict[str, Any]:
        if name not in self._tools:
            raise KeyError(f"Unknown tool: {name}")
        tool = self._tools[name]
        return await asyncio.wait_for(tool.handler(payload), timeout=tool.timeout_ms / 1000)
