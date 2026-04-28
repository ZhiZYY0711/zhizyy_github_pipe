from typing import Any

from app.tools.contracts import NormalizedToolResult
from app.tools.http_client import ToolHttpClient, build_tool_http_client
from app.tools.registry import ToolDefinition, ToolRegistry


def _payload_with_defaults(payload: dict[str, Any]) -> dict[str, Any]:
    normalized = dict(payload)
    normalized.setdefault("object_id", payload.get("raw_input"))
    normalized.setdefault("metric", "overview")
    return {key: value for key, value in normalized.items() if value is not None}


def _display_facts(raw: dict[str, Any]) -> list[dict[str, Any]]:
    facts = raw.get("facts")
    if isinstance(facts, list):
        return [_normalize_fact(fact) for fact in facts if isinstance(fact, dict)]

    metric = raw.get("metric")
    points = raw.get("points")
    if metric or isinstance(points, list):
        latest = points[-1].get("value") if isinstance(points, list) and points and isinstance(points[-1], dict) else None
        return [
            {
                "metric": metric,
                "window": raw.get("window"),
                "points": points or [],
                "context": raw.get("context", {}),
                "records": raw.get("records", []),
                "label": str(metric or "监测指标"),
                "value": "" if latest is None else str(latest),
                "status": str(raw.get("status") or "normal"),
            }
        ]

    evidence = raw.get("evidence")
    if isinstance(evidence, dict):
        return [
            {"label": str(key), "value": str(value), "status": "normal"}
            for key, value in evidence.items()
            if value is not None
        ][:6]

    return []


def _normalize_fact(fact: dict[str, Any]) -> dict[str, Any]:
    label = fact.get("label") or fact.get("metric") or fact.get("name") or "工具结果"
    value = fact.get("value") or fact.get("current") or fact.get("text") or ""
    return {
        **fact,
        "label": str(label),
        "value": str(value),
        "status": str(fact.get("status") or "normal"),
    }


def _normalize(
    tool_name: str,
    path: str,
    payload: dict[str, Any],
    raw: dict[str, Any],
) -> NormalizedToolResult:
    return {
        "tool_name": tool_name,
        "displayHint": tool_name,
        "summary": raw.get("summary") or f"{tool_name} completed",
        "input": payload,
        "context": raw.get("context") or {},
        "evidence": raw.get("evidence") or {},
        "facts": _display_facts(raw),
        "raw": raw,
        "raw_ref": {
            "path": path,
            "object_id": payload.get("object_id"),
            "source": "server-web",
        },
        "confidence": float(raw.get("confidence", 0.9)),
        "metadata": raw.get("metadata") or {"source": "server-web"},
    }


async def _invoke(
    tool_name: str,
    path: str,
    payload: dict[str, Any],
    client: ToolHttpClient | None = None,
) -> NormalizedToolResult:
    normalized_payload = _payload_with_defaults(payload)
    raw = await (client or build_tool_http_client()).get(path, normalized_payload)
    return _normalize(tool_name, path, normalized_payload, raw)


async def query_monitoring_trend(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_monitoring_trend",
        "/internal/virtual-expert/tools/monitoring-trend",
        payload,
        client,
    )


async def query_pipe_segment_context(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_pipe_segment_context",
        "/internal/virtual-expert/tools/pipe-segment-context",
        payload,
        client,
    )


async def query_equipment_status(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_equipment_status",
        "/internal/virtual-expert/tools/equipment-status",
        payload,
        client,
    )


async def query_related_tasks(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_related_tasks",
        "/internal/virtual-expert/tools/related-tasks",
        payload,
        client,
    )


async def search_similar_cases(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "search_similar_cases",
        "/internal/virtual-expert/tools/similar-cases",
        payload,
        client,
    )


async def resolve_operational_scope(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "resolve_operational_scope",
        "/internal/virtual-expert/tools/resolve-operational-scope",
        payload,
        client,
    )


async def query_monitoring_aggregate(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_monitoring_aggregate",
        "/internal/virtual-expert/tools/monitoring-aggregate",
        payload,
        client,
    )


async def query_monitoring_history(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_monitoring_history",
        "/internal/virtual-expert/tools/monitoring-history",
        payload,
        client,
    )


async def query_area_monitoring_summary(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_area_monitoring_summary",
        "/internal/virtual-expert/tools/area-monitoring-summary",
        payload,
        client,
    )


async def query_alarm_events(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_alarm_events",
        "/internal/virtual-expert/tools/alarm-events",
        payload,
        client,
    )


async def query_inspection_history(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_inspection_history",
        "/internal/virtual-expert/tools/inspection-history",
        payload,
        client,
    )


async def query_maintenance_history(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_maintenance_history",
        "/internal/virtual-expert/tools/maintenance-history",
        payload,
        client,
    )


async def query_topology_impact(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_topology_impact",
        "/internal/virtual-expert/tools/topology-impact",
        payload,
        client,
    )


async def query_emergency_plan(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_emergency_plan",
        "/internal/virtual-expert/tools/emergency-plan",
        payload,
        client,
    )


async def query_operation_logs(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_operation_logs",
        "/internal/virtual-expert/tools/operation-logs",
        payload,
        client,
    )


async def query_report_inventory(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_report_inventory",
        "/internal/virtual-expert/tools/report-inventory",
        payload,
        client,
    )


def build_business_registry() -> ToolRegistry:
    return ToolRegistry(
        [
            _tool(
                "query_monitoring_trend",
                "查询具体对象的监测指标快照或趋势。",
                "monitoring:read",
                query_monitoring_trend,
                "monitoring",
                "用户给出管段、设备、指标或短时间窗口，需要查看温度、压力、流量、振动等监测值时使用。",
                _schema({"object_id": "对象、管段或设备标识", "metric": "temperature/pressure/flow/vibration/overview"}),
            ),
            _tool(
                "query_pipe_segment_context",
                "查询管段所属管线、顺序、起止区域等拓扑上下文。",
                "pipeline:read",
                query_pipe_segment_context,
                "pipeline",
                "用户问题涉及管段位置、所属管线、上下文、起止区域或需要把管段放回管网结构中理解时使用。",
                _schema({"object_id": "管段或管线标识"}),
            ),
            _tool(
                "query_equipment_status",
                "查询管段或设备关联的设备状态列表。",
                "equipment:read",
                query_equipment_status,
                "equipment",
                "用户问题涉及传感器、阀门、设备状态、设备责任人或设备是否异常时使用。",
                _schema({"object_id": "管段或设备标识"}),
            ),
            _tool(
                "query_related_tasks",
                "查询管段或对象关联任务。",
                "task:read",
                query_related_tasks,
                "task",
                "用户需要了解巡检、维修、处置任务、任务状态或责任人时使用。",
                _schema({"object_id": "管段、设备或任务相关对象标识"}),
            ),
            _tool(
                "search_similar_cases",
                "查询相似案例。",
                "case:read",
                search_similar_cases,
                "case",
                "用户需要历史相似事件、处置经验或案例参考时使用。",
                _schema({"raw_input": "用户问题或事件描述", "object_id": "可选对象标识"}),
            ),
            _tool(
                "resolve_operational_scope",
                "把用户描述的区域、管线、管段、设备范围转换为可查询候选对象。",
                "pipeline:read",
                resolve_operational_scope,
                "scope",
                "问题包含山东省、某市、某管线、3号管段、阀室等区域或对象描述，但还没有明确 scope_id 时使用。",
                _schema({"raw_input": "原始用户问题", "scope_hint": "LLM 提取的区域、管线、管段或设备名称"}),
            ),
            _tool(
                "query_monitoring_aggregate",
                "按区域、管线、管段或设备聚合监测指标。",
                "monitoring:read",
                query_monitoring_aggregate,
                "monitoring",
                "需要平均温度、最高压力、年度/季度/月度统计、覆盖率、样本量等聚合指标时使用。",
                _schema(
                    {
                        "scope_type": "province/city/district/pipeline/segment/equipment",
                        "scope_id": "范围 ID",
                        "metric": "temperature/pressure/flow/vibration",
                        "start_time": "起始时间",
                        "end_time": "结束时间",
                        "aggregation": "avg/max/min/count",
                    },
                    ["scope_type", "scope_id", "metric"],
                ),
            ),
            _tool(
                "query_monitoring_history",
                "查询指定对象的历史监测序列。",
                "monitoring:read",
                query_monitoring_history,
                "monitoring",
                "需要一段时间内的监测明细、趋势序列、采样质量或曲线数据时使用。",
                _schema({"object_type": "对象类型", "object_id": "对象 ID", "metric": "指标", "start_time": "起始时间", "end_time": "结束时间", "granularity": "粒度"}),
            ),
            _tool(
                "query_area_monitoring_summary",
                "查询区域级监测总览和异常分布。",
                "monitoring:read",
                query_area_monitoring_summary,
                "monitoring",
                "用户按省、市、区问整体监测态势、异常数量、重点管段或区域级概览时使用。",
                _schema({"area_id": "区域 ID", "metric": "指标", "window": "时间窗口", "aggregation": "聚合方式"}),
            ),
            _tool(
                "query_alarm_events",
                "查询告警事件、等级、持续时间和闭环状态。",
                "alarm:read",
                query_alarm_events,
                "alarm",
                "用户询问告警、报警、高危事件、持续时间、是否闭环或告警列表时使用。",
                _schema({"scope": "查询范围", "time_range": "时间范围", "severity": "告警级别"}),
            ),
            _tool(
                "query_inspection_history",
                "查询巡检历史、异常记录和路线信息。",
                "inspection:read",
                query_inspection_history,
                "inspection",
                "用户关注巡检记录、漏检、现场复核、巡检人员或近期巡检异常时使用。",
                _schema({"scope": "查询范围", "time_range": "时间范围"}),
            ),
            _tool(
                "query_maintenance_history",
                "查询设备维修、维护和保养记录。",
                "maintenance:read",
                query_maintenance_history,
                "maintenance",
                "用户问题涉及设备维护历史、最近维修、保养状态或工单闭环时使用。",
                _schema({"equipment_id": "设备 ID", "scope": "可选范围", "time_range": "时间范围"}),
            ),
            _tool(
                "query_topology_impact",
                "分析对象上下游和影响范围。",
                "pipeline:read",
                query_topology_impact,
                "pipeline",
                "用户询问某管段、阀门、站点异常会影响哪里、如何隔离或上下游关系时使用。",
                _schema({"object_type": "对象类型", "object_id": "对象 ID"}),
            ),
            _tool(
                "query_emergency_plan",
                "查询应急预案、处置卡和联系人。",
                "emergency:read",
                query_emergency_plan,
                "emergency",
                "用户需要应急处置步骤、预案、联系人、响应等级或现场动作清单时使用。",
                _schema({"incident_type": "事件类型", "scope": "范围", "severity": "严重程度"}),
            ),
            _tool(
                "query_operation_logs",
                "查询操作日志和关键操作链。",
                "log:read",
                query_operation_logs,
                "audit",
                "用户需要追溯谁做了什么、最近操作、误操作风险或审计链路时使用。",
                _schema({"user": "用户", "object": "对象", "time_range": "时间范围", "operation": "操作类型"}),
            ),
            _tool(
                "query_report_inventory",
                "查询历史报告、导出记录和附件清单。",
                "report:read",
                query_report_inventory,
                "report",
                "用户需要查找已生成报告、历史导出、附件或归档记录时使用。",
                _schema({"session_id": "会话 ID", "time_range": "时间范围", "format": "pdf/excel"}),
            ),
        ]
    )


def _tool(
    name: str,
    description: str,
    permission: str,
    handler,
    category: str,
    when_to_use: str,
    input_schema: dict[str, Any],
) -> ToolDefinition:
    return ToolDefinition(
        name=name,
        description=description,
        permission=permission,
        timeout_ms=8000,
        requires_confirmation=False,
        handler=handler,
        category=category,
        when_to_use=when_to_use,
        input_schema=input_schema,
        output_schema={
            "type": "object",
            "properties": {
                "summary": {"type": "string"},
                "facts": {"type": "array"},
                "records": {"type": "array"},
                "context": {"type": "object"},
                "confidence": {"type": "number"},
                "metadata": {"type": "object"},
            },
        },
    )


def _schema(properties: dict[str, str], required: list[str] | None = None) -> dict[str, Any]:
    return {
        "type": "object",
        "properties": {key: {"type": "string", "description": description} for key, description in properties.items()},
        "required": required or [],
    }
