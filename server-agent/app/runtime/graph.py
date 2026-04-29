from dataclasses import dataclass
from typing import Awaitable, Callable

from app.agent.state import AgentState


RuntimeNode = Callable[[AgentState], Awaitable[AgentState]]


@dataclass(frozen=True)
class RuntimeGraph:
    nodes: tuple[RuntimeNode, ...]

    async def run(self, state: AgentState):
        current = state
        for node in self.nodes:
            current = await node(current)
            yield current


def build_phase1_graph(*nodes: RuntimeNode) -> RuntimeGraph:
    return RuntimeGraph(tuple(nodes))
