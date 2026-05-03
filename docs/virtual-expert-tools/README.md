# 虚拟专家 Agent 工具能力清单与扩展规划

本文档梳理虚拟专家 Agent 当前已接入的工具能力，以及后续按“宽口径数据发现 -> 区域总览 -> 异常发现 -> 钻取分析 -> 报告导出”的方向扩展的工具规划。当前代码侧已经接入 P0 工具和一轮多 action 编排，文档状态以 `server-agent/app/tools/business_tools.py` 与 ReAct runtime 为准。

## 目标

- 让 Agent 面对“帮我查一下山东济南的一些数据”这类宽泛问题时，先主动发现可查数据，而不是直接要求用户补条件。
- 让 Agent 能从区域、对象、指标、异常、任务、报告多个入口组织查询链路。
- 支持一轮 ReAct 内连续调用多个工具或知识库，减少交互迟滞。
- 让导出报告复用工具证据，减少内部事件噪声，提升可读性。

## 目录

- [01-范围解析与数据发现](01-scope-and-data-discovery.md)
- [02-监测与区域运行数据](02-monitoring-and-area-data.md)
- [03-资产拓扑与设备](03-assets-topology-equipment.md)
- [04-告警巡检任务与运维记录](04-events-tasks-operations.md)
- [05-知识库案例与记忆](05-knowledge-cases-memory.md)
- [06-报告导出与证据包](06-report-export-evidence.md)
- [07-ReAct 编排能力](07-react-orchestration.md)

## 优先级建议

| 优先级 | 能力 | 价值 |
| --- | --- | --- |
| P0 | `resolve_area_scope`、`query_area_data_catalog`、`query_area_operational_overview` | 已接入，解决宽泛地域查询无法推进的问题 |
| P0 | 一轮 ReAct 多 action | 已接入，一次查询中串联区域解析、目录、总览、告警摘要 |
| P1 | `find_recent_anomalies`、`find_top_risk_segments`、`find_unclosed_alarms` | 已接入，让 Agent 主动发现风险 |
| P1 | TXT、Markdown、DOCX 导出 | 已接入导出计划与前端快捷入口，覆盖归档、编辑、流转场景 |
| P2 | 对比分析、趋势变化检测、证据包生成 | 提升分析深度和报告质量 |

## 当前已接入工具

| 类别 | 工具 | 状态 |
| --- | --- | --- |
| 范围发现 | `resolve_operational_scope`、`resolve_area_scope`、`resolve_asset_scope`、`query_area_data_catalog`、`suggest_next_queries` | 已接入 |
| 区域与监测 | `query_area_operational_overview`、`query_monitoring_trend`、`query_monitoring_aggregate`、`query_monitoring_history`、`query_area_monitoring_summary`、`query_area_alarm_summary`、`find_recent_anomalies`、`find_top_risk_segments` | 已接入 |
| 资产拓扑 | `query_pipe_segment_context`、`query_equipment_status`、`query_topology_impact`、`query_area_asset_summary`、`query_asset_relationships` | 已接入 |
| 告警任务运维 | `query_alarm_events`、`find_unclosed_alarms`、`find_stale_inspections`、`query_area_task_summary`、`query_inspection_history`、`query_related_tasks`、`query_maintenance_history`、`query_operation_logs`、`trace_operation_chain`、`query_emergency_plan` | 已接入 |
| 知识与案例 | `knowledge_search`、`memory_search`、`search_similar_cases`、`query_case_library`、`query_sop_by_scenario`、`remember_analysis_preference` | 已接入 |
| 报告导出 | `query_report_inventory`、`build_evidence_pack`、`build_report_outline`、`generate_table_from_observations`、`validate_report_completeness`、`create_export_report` | 已接入，支持 `pdf`、`excel`、`txt`、`md`、`docx` |

## 数据深化方向

- 风险发现、案例库和 SOP 已有可调用实现，后续重点是接入更真实、更细颗粒的数据源。
- 证据包、报告大纲和报告完整性校验已作为 Agent 工具接入，后续可继续和导出模板深度联动。
- 相似案例、SOP 当前以种子知识和轻量规则为主，仍需要持续补充运维复盘数据。
