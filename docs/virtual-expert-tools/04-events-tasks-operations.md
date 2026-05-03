# 04-告警巡检任务与运维记录

## 已有工具

| 工具 | 状态 | 作用 | 适用问题 |
| --- | --- | --- | --- |
| `query_alarm_events` | 已接入 | 查询告警事件、等级、持续时间和闭环状态 | “近 24 小时有什么告警” |
| `query_inspection_history` | 已接入 | 查询巡检历史、异常记录和路线信息 | “近期巡检有没有异常” |
| `query_related_tasks` | 已接入 | 查询管段或对象关联任务 | “这个问题谁在处理” |
| `query_maintenance_history` | 已接入 | 查询设备维修、维护和保养记录 | “这个设备最近修过吗” |
| `query_operation_logs` | 已接入 | 查询操作日志和关键操作链 | “谁操作过这个阀门” |
| `query_emergency_plan` | 已接入 | 查询应急预案、处置卡和联系人 | “现场怎么处置” |

## 已接入工具

### `find_unclosed_alarms`

查询未闭环告警。

输出建议：

- 告警对象。
- 告警等级。
- 持续时长。
- 当前责任人或处理状态。
- 推荐处置动作。

### `find_stale_inspections`

查询超期未巡检对象。

输出建议：

- 对象名称。
- 上次巡检时间。
- 超期天数。
- 所属区域和责任人。

### `query_area_task_summary`

区域任务概览。

输出建议：

- 待处理任务数。
- 进行中任务数。
- 超期任务数。
- 与高风险管段相关的任务。

### `trace_operation_chain`

把监测异常、告警、任务、操作日志串成链路。

用途：

- 追溯“异常发生前后谁做了什么”。
- 支撑报告里的“过程复盘”。

## 默认策略

当区域总览发现异常时，Agent 应优先补查：

1. `find_unclosed_alarms`
2. `query_area_task_summary`
3. `query_inspection_history`

这样最终回答不是只有指标，还能给出处置状态。
