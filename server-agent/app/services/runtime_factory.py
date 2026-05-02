from collections.abc import Callable
from typing import Any

from app.agent.service import AgentRuntimeService
from app.tools.business_tools import build_business_registry


def build_runtime_service(
    summary_memory: list[str] | None = None,
    preference_memory: list[dict] | None = None,
    *,
    runtime_cls: type[AgentRuntimeService] | Callable[..., AgentRuntimeService] = AgentRuntimeService,
    registry_factory: Callable[[], Any] = build_business_registry,
) -> AgentRuntimeService:
    tool_registry = registry_factory()
    try:
        return runtime_cls(
            tool_registry=tool_registry,
            summary_memory=summary_memory or [],
            preference_memory=preference_memory or [],
        )
    except TypeError:
        try:
            return runtime_cls(
                tool_registry=tool_registry,
                summary_memory=summary_memory or [],
            )
        except TypeError:
            return runtime_cls(tool_registry=tool_registry)
