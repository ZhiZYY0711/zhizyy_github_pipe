# 02-监测与区域运行数据

## 已有工具

| 工具 | 状态 | 作用 | 适用问题 |
| --- | --- | --- | --- |
| `query_monitoring_trend` | 已接入 | 查询具体管段或设备的指标快照/趋势 | “3 号管段压力趋势怎么样” |
| `query_monitoring_aggregate` | 已接入 | 按区域、管线、管段或设备聚合监测指标 | “近 7 天平均压力是多少” |
| `query_monitoring_history` | 已接入 | 查询指定对象的历史监测序列 | “这个传感器昨天的数据明细” |
| `query_area_monitoring_summary` | 已接入 | 区域级监测总览和异常分布 | “济南整体监测态势” |

## 已接入工具

### `query_area_operational_overview`

区域运行总览，作为宽泛查询的默认落点。

输入建议：

```json
{
  "area_id": "370100",
  "scope_type": "city",
  "time_range": "24h"
}
```

输出建议：

- 监测数据总量、正常/危险/严重数量。
- 重点异常管段。
- 设备状态摘要。
- 告警和巡检摘要。
- 最近更新时间。

## 已接入风险发现工具

### `query_area_alarm_summary`

区域告警统计。

输出建议：

- 告警总数。
- 严重/危险/一般分布。
- 未闭环告警数。
- Top 告警对象。

### `find_recent_anomalies`

主动发现近期异常。

输入建议：

```json
{
  "scope": "area:370100",
  "time_range": "24h",
  "limit": 10
}
```

输出建议：

- 异常对象。
- 指标名称。
- 当前值、阈值、异常等级。
- 发生时间。

### `find_top_risk_segments`

按风险排序返回重点管段。

排序依据可组合：

- 严重告警数。
- 异常监测点数。
- 近期巡检异常数。
- 设备维护超期。

## 默认策略

当用户说“查某地数据”但没有指定指标时：

1. 先查 `query_area_operational_overview`。
2. 如发现异常，再补查 `find_recent_anomalies`。
3. 如果用户追问具体指标，再切到 `query_monitoring_aggregate` 或 `query_monitoring_history`。
