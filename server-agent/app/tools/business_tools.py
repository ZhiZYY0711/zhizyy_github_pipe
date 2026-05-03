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


async def resolve_area_scope(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "resolve_area_scope",
        "/internal/virtual-expert/tools/resolve-area-scope",
        payload,
        client,
    )


async def query_area_data_catalog(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_area_data_catalog",
        "/internal/virtual-expert/tools/area-data-catalog",
        payload,
        client,
    )


async def query_area_operational_overview(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_area_operational_overview",
        "/internal/virtual-expert/tools/area-operational-overview",
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


async def query_area_alarm_summary(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_area_alarm_summary",
        "/internal/virtual-expert/tools/area-alarm-summary",
        payload,
        client,
    )


async def find_recent_anomalies(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "find_recent_anomalies",
        "/internal/virtual-expert/tools/recent-anomalies",
        payload,
        client,
    )


async def find_top_risk_segments(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "find_top_risk_segments",
        "/internal/virtual-expert/tools/top-risk-segments",
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


async def find_unclosed_alarms(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "find_unclosed_alarms",
        "/internal/virtual-expert/tools/unclosed-alarms",
        payload,
        client,
    )


async def find_stale_inspections(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "find_stale_inspections",
        "/internal/virtual-expert/tools/stale-inspections",
        payload,
        client,
    )


async def query_area_task_summary(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_area_task_summary",
        "/internal/virtual-expert/tools/area-task-summary",
        payload,
        client,
    )


async def trace_operation_chain(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "trace_operation_chain",
        "/internal/virtual-expert/tools/operation-chain",
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


async def resolve_asset_scope(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "resolve_asset_scope",
        "/internal/virtual-expert/tools/resolve-asset-scope",
        payload,
        client,
    )


async def query_area_asset_summary(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_area_asset_summary",
        "/internal/virtual-expert/tools/area-asset-summary",
        payload,
        client,
    )


async def query_asset_relationships(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_asset_relationships",
        "/internal/virtual-expert/tools/asset-relationships",
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


async def query_case_library(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_case_library",
        "/internal/virtual-expert/tools/case-library",
        payload,
        client,
    )


async def query_sop_by_scenario(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    return await _invoke(
        "query_sop_by_scenario",
        "/internal/virtual-expert/tools/sop-by-scenario",
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


async def suggest_next_queries(payload: dict[str, Any]) -> NormalizedToolResult:
    raw_input = str(payload.get("raw_input") or payload.get("question") or "")
    area = str(payload.get("area_name") or payload.get("area_id") or "当前范围")
    suggestions = [
        f"查看{area}近 24 小时异常监测",
        f"查看{area}未闭环告警和待处理任务",
        f"查看{area}风险最高的管段",
        f"导出{area}运行概览报告",
    ]
    return _local_result(
        "suggest_next_queries",
        "已生成下一步查询建议。",
        payload,
        [{"label": "建议", "value": item, "status": "normal"} for item in suggestions],
        [{"record_type": "next_query", "query": item, "raw_input": raw_input} for item in suggestions],
    )


async def build_evidence_pack(payload: dict[str, Any]) -> NormalizedToolResult:
    observations = payload.get("observations")
    records = observations if isinstance(observations, list) else []
    facts = [
        {"label": "证据数量", "value": str(len(records)), "status": "normal"},
        {"label": "包含知识证据", "value": str(bool(payload.get("knowledge"))), "status": "normal"},
    ]
    return _local_result("build_evidence_pack", "已整理证据包。", payload, facts, records[:20])


async def remember_analysis_preference(payload: dict[str, Any]) -> NormalizedToolResult:
    content = str(payload.get("content") or payload.get("preference") or "")
    return _local_result(
        "remember_analysis_preference",
        "已提取分析偏好候选，运行结束后进入偏好确认流程。",
        payload,
        [{"label": "偏好", "value": content or "未提供", "status": "normal"}],
        [{"record_type": "analysis_preference", "content": content, "status": "pending"}],
    )


async def build_report_outline(payload: dict[str, Any]) -> NormalizedToolResult:
    sections = payload.get("sections")
    if not isinstance(sections, list) or not sections:
        sections = ["用户问题", "数据范围", "关键发现", "风险判断", "处置建议", "证据表", "待补充信息"]
    return _local_result(
        "build_report_outline",
        "已生成报告大纲。",
        payload,
        [{"label": "章节数", "value": str(len(sections)), "status": "normal"}],
        [{"record_type": "report_section", "order": index + 1, "title": str(title)} for index, title in enumerate(sections)],
    )


async def generate_table_from_observations(payload: dict[str, Any]) -> NormalizedToolResult:
    observations = payload.get("observations")
    rows = observations if isinstance(observations, list) else []
    normalized_rows = [
        row if isinstance(row, dict) else {"value": row}
        for row in rows[:50]
    ]
    return _local_result(
        "generate_table_from_observations",
        "已将观察结果整理为表格行。",
        payload,
        [{"label": "行数", "value": str(len(normalized_rows)), "status": "normal"}],
        normalized_rows,
    )


async def validate_report_completeness(payload: dict[str, Any]) -> NormalizedToolResult:
    checks = {
        "用户问题": bool(payload.get("raw_input") or payload.get("question")),
        "数据范围": bool(payload.get("scope") or payload.get("area_id") or payload.get("object_id")),
        "证据": bool(payload.get("observations") or payload.get("evidence")),
        "风险判断": bool(payload.get("risk") or payload.get("judgment")),
        "建议": bool(payload.get("actions") or payload.get("recommendations")),
    }
    records = [{"record_type": "report_check", "item": key, "passed": value} for key, value in checks.items()]
    missing = [key for key, value in checks.items() if not value]
    return _local_result(
        "validate_report_completeness",
        "报告完整性检查完成。" if not missing else f"报告仍缺少：{'、'.join(missing)}。",
        payload,
        [{"label": key, "value": "通过" if value else "缺失", "status": "normal" if value else "warning"} for key, value in checks.items()],
        records,
    )


async def create_export_report(
    payload: dict[str, Any], client: ToolHttpClient | None = None
) -> NormalizedToolResult:
    export_plan = _export_plan_from_payload(payload)
    format_label = {
        "excel": "Excel",
        "txt": "TXT",
        "md": "Markdown",
        "docx": "DOCX",
    }.get(export_plan["format"], "PDF")
    return {
        "tool_name": "create_export_report",
        "displayHint": "create_export_report",
        "summary": f"已准备{format_label}导出计划，运行完成后生成文件。",
        "input": payload,
        "context": {
            "export_plan": export_plan,
            "deferred_until_run_completed": True,
        },
        "evidence": {},
        "facts": [
            {"label": "导出格式", "value": export_plan["format"], "status": "normal"},
            {"label": "导出范围", "value": export_plan["scope"], "status": "normal"},
            {"label": "报告标题", "value": export_plan["title"], "status": "normal"},
        ],
        "raw": {
            "status": "prepared",
            "export_plan": export_plan,
        },
        "raw_ref": {
            "path": "agent://export/create_report",
            "source": "server-agent",
        },
        "confidence": 0.95,
        "metadata": {
            "source": "server-agent",
            "capability_status": "export_plan_prepared",
            "export_requested": True,
        },
    }


def _local_result(
    tool_name: str,
    summary: str,
    payload: dict[str, Any],
    facts: list[dict[str, Any]],
    records: list[dict[str, Any]],
) -> NormalizedToolResult:
    return {
        "tool_name": tool_name,
        "displayHint": tool_name,
        "summary": summary,
        "input": payload,
        "context": {},
        "evidence": {},
        "facts": facts,
        "raw": {"records": records, "summary": summary},
        "raw_ref": {
            "path": f"agent://{tool_name}",
            "source": "server-agent",
        },
        "confidence": 0.9,
        "metadata": {"source": "server-agent", "capability_status": "local_agent_tool"},
    }


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
                "resolve_area_scope",
                "将山东、济南、历下区等自然语言地名解析为系统区域范围。",
                "pipeline:read",
                resolve_area_scope,
                "discovery",
                "用户给出省、市、区等地理范围但没有明确 area_id 时优先使用。",
                _schema({"raw_input": "原始用户问题", "province": "省份名称", "city": "城市名称", "district": "区县名称"}),
            ),
            _tool(
                "query_area_data_catalog",
                "查询区域下可用的数据目录和代表性对象。",
                "monitoring:read",
                query_area_data_catalog,
                "discovery",
                "用户宽泛询问某地有什么数据、能查哪些数据、或者没有指定具体指标时使用。",
                _schema({"area_id": "区域 ID", "scope_type": "province/city/district", "include_samples": "是否返回代表性对象"}),
            ),
            _tool(
                "query_area_operational_overview",
                "查询区域运行总览，包括监测、异常、管段候选等概况。",
                "monitoring:read",
                query_area_operational_overview,
                "monitoring",
                "用户按区域宽泛询问运行态势、整体数据、异常分布或概览时使用。",
                _schema({"area_id": "区域 ID", "scope_type": "province/city/district", "time_range": "时间范围"}),
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
                "query_area_alarm_summary",
                "查询区域告警统计、等级分布和未闭环概况。",
                "alarm:read",
                query_area_alarm_summary,
                "alarm",
                "用户按区域询问告警数量、严重/危险/一般分布、未闭环告警或告警概览时使用。",
                _schema({"area_id": "区域 ID", "scope_type": "province/city/district", "time_range": "时间范围"}),
            ),
            _tool(
                "find_recent_anomalies",
                "主动发现近期异常监测对象。",
                "monitoring:read",
                find_recent_anomalies,
                "monitoring",
                "用户希望发现异常、近期风险、Top 异常对象或宽泛查询后需要补充异常明细时使用。",
                _schema({"scope": "查询范围", "time_range": "时间范围", "limit": "返回数量"}),
            ),
            _tool(
                "find_top_risk_segments",
                "按风险排序返回重点管段。",
                "monitoring:read",
                find_top_risk_segments,
                "monitoring",
                "用户需要重点风险管段、优先排查对象或风险排序时使用。",
                _schema({"scope": "查询范围", "time_range": "时间范围", "limit": "返回数量"}),
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
                "find_unclosed_alarms",
                "查询未闭环告警。",
                "alarm:read",
                find_unclosed_alarms,
                "alarm",
                "用户询问未处理、未闭环、仍需跟进的告警时使用。",
                _schema({"scope": "查询范围", "time_range": "时间范围", "severity": "告警级别"}),
            ),
            _tool(
                "find_stale_inspections",
                "查询超期未巡检或长期未检查对象。",
                "inspection:read",
                find_stale_inspections,
                "inspection",
                "用户关注漏检、超期巡检、长期未检查设备或巡检覆盖风险时使用。",
                _schema({"scope": "查询范围", "stale_days": "超期天数", "limit": "返回数量"}),
            ),
            _tool(
                "query_area_task_summary",
                "查询区域任务概览和待处理任务分布。",
                "task:read",
                query_area_task_summary,
                "task",
                "用户按区域询问待处理、进行中、超期任务或任务处置状态时使用。",
                _schema({"area_id": "区域 ID", "time_range": "时间范围"}),
            ),
            _tool(
                "trace_operation_chain",
                "串联监测异常、告警、任务和操作日志形成过程链。",
                "operations:read",
                trace_operation_chain,
                "audit",
                "用户需要追溯异常发生前后谁做了什么、处置链路、复盘时间线时使用。",
                _schema({"scope": "查询范围", "time_range": "时间范围", "incident_id": "事件或告警 ID"}),
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
                "resolve_asset_scope",
                "解析自然语言资产对象为候选管线、管段、设备或站点。",
                "pipeline:read",
                resolve_asset_scope,
                "scope",
                "用户给出资产名称、编号、阀室、站点、传感器但没有明确 object_id 时使用。",
                _schema({"raw_input": "原始用户问题", "asset_hint": "资产名称或编号", "object_type": "pipeline/segment/equipment/station"}),
            ),
            _tool(
                "query_area_asset_summary",
                "查询区域资产总览。",
                "equipment:read",
                query_area_asset_summary,
                "equipment",
                "用户询问区域下管线、管段、设备、传感器数量和异常资产概况时使用。",
                _schema({"area_id": "区域 ID", "scope_type": "province/city/district"}),
            ),
            _tool(
                "query_asset_relationships",
                "查询区域、管线、管段和设备的层级关系。",
                "pipeline:read",
                query_asset_relationships,
                "pipeline",
                "需要为分析或报告补充资产上下文、上下级关系、区域到设备链路时使用。",
                _schema({"object_type": "对象类型", "object_id": "对象 ID", "include_equipment": "是否包含设备"}),
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
                "query_case_library",
                "按区域、指标、告警类型或设备类型查询历史案例。",
                "case:read",
                query_case_library,
                "case",
                "用户需要历史案例、处置经验、复盘参考或相似事件时使用。",
                _schema({"scenario": "场景", "metric": "指标", "scope": "范围", "limit": "返回数量"}),
            ),
            _tool(
                "query_sop_by_scenario",
                "按场景查询 SOP 和处置规范。",
                "case:read",
                query_sop_by_scenario,
                "knowledge",
                "业务数据不足但仍需按规范给出处置建议，或用户明确询问 SOP/流程时使用。",
                _schema({"scenario": "压力异常/流量突降/传感器离线/阀门异常/未闭环告警", "severity": "严重程度"}),
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
                _schema({"session_id": "会话 ID", "time_range": "时间范围", "format": "pdf/excel/txt/md/docx"}),
            ),
            _tool(
                "suggest_next_queries",
                "根据当前范围和数据目录生成下一步查询建议。",
                "monitoring:read",
                suggest_next_queries,
                "discovery",
                "宽泛查询完成后，需要给用户几个自然的继续下钻方向时使用。",
                _schema({"raw_input": "原始用户问题", "area_id": "区域 ID", "area_name": "区域名称"}),
            ),
            _tool(
                "build_evidence_pack",
                "将工具结果和知识命中合并成证据包。",
                "report:read",
                build_evidence_pack,
                "report",
                "生成最终回答或导出报告前，需要整理可引用证据时使用。",
                _schema({"observations": "工具观察列表", "knowledge": "知识命中列表"}),
            ),
            _tool(
                "remember_analysis_preference",
                "提取用户偏好的分析方式作为记忆候选。",
                "report:read",
                remember_analysis_preference,
                "memory",
                "用户表达输出、分析、排序、报告偏好时使用；最终是否保存仍由记忆流程确认。",
                _schema({"content": "偏好内容", "reason": "提取原因"}),
            ),
            _tool(
                "build_report_outline",
                "根据用户目的生成报告大纲。",
                "report:read",
                build_report_outline,
                "report",
                "用户要求导出正式报告、复盘材料或需要先组织报告结构时使用。",
                _schema({"purpose": "报告用途", "sections": "期望章节"}),
            ),
            _tool(
                "generate_table_from_observations",
                "把工具观察结果整理为报告表格。",
                "report:read",
                generate_table_from_observations,
                "report",
                "报告或 Excel 导出前需要把业务观察转成表格行时使用。",
                _schema({"observations": "工具观察列表", "table_type": "表格类型"}),
            ),
            _tool(
                "validate_report_completeness",
                "检查报告是否包含问题、范围、证据、判断和建议。",
                "report:read",
                validate_report_completeness,
                "report",
                "导出前检查报告完整性，或用户要求正式材料时使用。",
                _schema({"raw_input": "用户问题", "scope": "范围", "observations": "证据", "judgment": "判断", "recommendations": "建议"}),
            ),
            _tool(
                "create_export_report",
                "准备虚拟专家会话导出文件。",
                "report:write",
                create_export_report,
                "report",
                "用户明确要求导出 PDF、Excel、报告、复盘表或下载当前/历史研判内容时使用；如果用户同时要求先分析再导出，应先完成必要分析和证据收集，再调用此工具。",
                _export_schema(),
                requires_confirmation=True,
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
    requires_confirmation: bool = False,
) -> ToolDefinition:
    return ToolDefinition(
        name=name,
        description=description,
        permission=permission,
        timeout_ms=8000,
        requires_confirmation=requires_confirmation,
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


def _export_schema() -> dict[str, Any]:
    return {
        "type": "object",
        "properties": {
            "format": {"type": "string", "enum": ["pdf", "excel", "txt", "md", "docx"], "description": "导出格式"},
            "scope": {
                "type": "string",
                "enum": ["agent_selected", "latest_turn", "full_session", "all_conversations"],
                "description": "导出范围",
            },
            "title": {"type": "string", "description": "文件标题"},
            "audience": {"type": "string", "description": "报告面向对象"},
            "purpose": {"type": "string", "description": "导出用途"},
            "detail_level": {"type": "string", "enum": ["brief", "standard", "detailed"], "description": "内容详略"},
            "include_evidence": {"type": "boolean", "description": "是否包含证据链"},
            "include_timeline": {"type": "boolean", "description": "是否包含过程时间线"},
            "sections": {"type": "array", "items": {"type": "object"}, "description": "报告章节"},
            "tables": {"type": "array", "items": {"type": "object"}, "description": "Excel 或报告表格"},
        },
        "required": ["format"],
    }


def _export_plan_from_payload(payload: dict[str, Any]) -> dict[str, Any]:
    format_value = str(payload.get("format") or "pdf").lower()
    if format_value == "markdown":
        format_value = "md"
    if format_value in {"word", "doc"}:
        format_value = "docx"
    if format_value not in {"pdf", "excel", "txt", "md", "docx"}:
        format_value = "pdf"
    scope = str(payload.get("scope") or "latest_turn")
    if scope not in {"agent_selected", "latest_turn", "full_session", "all_conversations"}:
        scope = "latest_turn"
    detail_level = str(payload.get("detail_level") or payload.get("detailLevel") or "standard")
    if detail_level not in {"brief", "standard", "detailed"}:
        detail_level = "standard"
    return {
        "format": format_value,
        "scope": scope,
        "title": str(payload.get("title") or ("虚拟专家研判明细" if format_value == "excel" else "虚拟专家研判报告")),
        "style": str(payload.get("style") or ("freeform_workbook" if format_value == "excel" else "standard_report")),
        "audience": str(payload.get("audience") or "现场处置人员"),
        "purpose": str(payload.get("purpose") or "研判归档"),
        "tone": str(payload.get("tone") or "清晰直接"),
        "detailLevel": detail_level,
        "sections": _list_of_dicts(payload.get("sections")),
        "tables": _list_of_dicts(payload.get("tables")),
        "evidencePolicy": str(payload.get("evidence_policy") or payload.get("evidencePolicy") or "llm_selected"),
        "includeEvidence": _bool_value(payload.get("include_evidence", payload.get("includeEvidence")), True),
        "includeTimeline": _bool_value(payload.get("include_timeline", payload.get("includeTimeline")), True),
        "requiresConfirmation": False,
        "reason": str(payload.get("reason") or "ReAct 已选择导出工具。"),
    }


def _list_of_dicts(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        return []
    return [item for item in value if isinstance(item, dict)]


def _bool_value(value: Any, fallback: bool) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.lower() in {"true", "1", "yes", "y", "是"}
    return fallback
