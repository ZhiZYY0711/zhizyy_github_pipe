from typing import Any

from app.tools.registry import ToolDefinition, ToolRegistry


async def _mock_result(tool_name: str, payload: dict[str, Any], summary: str) -> dict[str, Any]:
    return {
        "tool_name": tool_name,
        "summary": summary,
        "input": payload,
        "context": {
            "object_type": payload.get("object_type"),
            "object_id": payload.get("object_id"),
            "metric": payload.get("metric"),
        },
        "evidence": {
            "title": summary,
            "confidence": 0.72,
            "relevance_score": 0.81,
        },
    }


async def query_monitoring_trend(payload: dict[str, Any]) -> dict[str, Any]:
    return await _mock_result("query_monitoring_trend", payload, "近24小时指标存在持续波动，需要复核趋势。")


async def query_pipe_segment_context(payload: dict[str, Any]) -> dict[str, Any]:
    return await _mock_result("query_pipe_segment_context", payload, "已定位关联管段、区域和上下游对象。")


async def query_equipment_status(payload: dict[str, Any]) -> dict[str, Any]:
    return await _mock_result("query_equipment_status", payload, "关联设备存在最近维护记录。")


async def query_related_tasks(payload: dict[str, Any]) -> dict[str, Any]:
    return await _mock_result("query_related_tasks", payload, "近7天存在相关巡检任务。")


async def search_similar_cases(payload: dict[str, Any]) -> dict[str, Any]:
    return await _mock_result("search_similar_cases", payload, "找到2条相似异常案例。")


async def retrieve_domain_knowledge(payload: dict[str, Any]) -> dict[str, Any]:
    return await _mock_result("retrieve_domain_knowledge", payload, "检索到相关巡检规范和处置建议。")


def build_mock_registry() -> ToolRegistry:
    return ToolRegistry(
        [
            ToolDefinition("query_monitoring_trend", "查询监测趋势", "monitoring:read", 8000, False, query_monitoring_trend),
            ToolDefinition("query_pipe_segment_context", "查询管段上下文", "pipeline:read", 8000, False, query_pipe_segment_context),
            ToolDefinition("query_equipment_status", "查询设备状态", "equipment:read", 8000, False, query_equipment_status),
            ToolDefinition("query_related_tasks", "查询关联任务", "task:read", 8000, False, query_related_tasks),
            ToolDefinition("search_similar_cases", "查询相似案例", "case:read", 8000, False, search_similar_cases),
            ToolDefinition("retrieve_domain_knowledge", "检索领域知识", "knowledge:read", 8000, False, retrieve_domain_knowledge),
        ]
    )
