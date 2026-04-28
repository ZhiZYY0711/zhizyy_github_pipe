from typing import Any, TypedDict


class NormalizedToolResult(TypedDict):
    tool_name: str
    displayHint: str
    summary: str
    input: dict[str, Any]
    context: dict[str, Any]
    evidence: dict[str, Any]
    facts: list[dict[str, Any]]
    raw: dict[str, Any]
    raw_ref: dict[str, Any]
    confidence: float
    metadata: dict[str, Any]
