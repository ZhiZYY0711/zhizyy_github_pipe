import asyncio

import pytest

from app.tools.business_tools import build_business_registry
from app.tools.registry import ToolDefinition, ToolRegistry


def test_registry_lists_allowed_tools_by_permission():
    registry = build_business_registry()
    tools = registry.available_tools({"monitoring:read", "task:read"})
    names = {tool.name for tool in tools}

    assert "query_monitoring_trend" in names
    assert "query_related_tasks" in names
    assert "query_equipment_status" not in names


def test_registry_rejects_duplicate_tool_names():
    with pytest.raises(ValueError, match="Duplicate tool name: duplicate_tool"):
        ToolRegistry(
            [
                ToolDefinition("duplicate_tool", "first", "monitoring:read", 1000, False, lambda payload: None),
                ToolDefinition("duplicate_tool", "second", "task:read", 1000, False, lambda payload: None),
            ]
        )


def test_business_registry_exposes_phase1_read_only_tools():
    registry = build_business_registry()

    assert {tool.name for tool in registry.available_tools({"equipment:read", "task:read"})} == {
        "query_equipment_status",
        "query_related_tasks",
    }


def test_business_registry_exposes_expanded_agent_tool_catalog():
    registry = build_business_registry()
    tools = registry.available_tools(
        {
            "monitoring:read",
            "pipeline:read",
            "equipment:read",
            "task:read",
            "case:read",
            "alarm:read",
            "inspection:read",
            "maintenance:read",
            "emergency:read",
            "log:read",
            "report:read",
        }
    )
    by_name = {tool.name: tool for tool in tools}

    assert {
        "resolve_operational_scope",
        "query_monitoring_aggregate",
        "query_monitoring_history",
        "query_area_monitoring_summary",
        "query_alarm_events",
        "query_inspection_history",
        "query_maintenance_history",
        "query_topology_impact",
        "query_emergency_plan",
        "query_operation_logs",
        "query_report_inventory",
    }.issubset(by_name)
    assert by_name["query_monitoring_aggregate"].category == "monitoring"
    assert "metric" in by_name["query_monitoring_aggregate"].input_schema["properties"]
    assert "区域" in by_name["resolve_operational_scope"].when_to_use


@pytest.mark.asyncio
async def test_registry_applies_tool_timeout():
    async def slow_tool(payload):
        await asyncio.sleep(0.05)
        return {"summary": "too slow"}

    registry = ToolRegistry(
        [
            ToolDefinition("slow_tool", "Slow tool", "tool:read", 1, False, slow_tool),
        ]
    )

    with pytest.raises(TimeoutError):
        await registry.invoke("slow_tool", {})
