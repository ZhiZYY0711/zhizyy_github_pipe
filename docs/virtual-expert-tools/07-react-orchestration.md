# 07-ReAct 编排能力

## 现状

当前 ReAct runtime 已支持单个 `action` 和 `actions[]` 一轮多 action 两种模式。一个典型流程既可以多轮推进，也可以在范围、目录、总览这些依赖明确的步骤中合并执行：

1. 解析区域。
2. 查询数据目录。
3. 查询区域总览。
4. 查询告警或异常。
5. 生成最终回答。

一轮多 action 解决两个问题：

- 减少中间等待。
- 让宽泛地域查询能一次串联范围解析、数据目录和区域总览。

## 已接入能力

### 一轮多 action

在兼容现有 `action` 字段的基础上，增加 `actions` 数组。

示例：

```json
{
  "actions": [
    {
      "action": "tool_call",
      "tool_name": "resolve_area_scope",
      "tool_input": {
        "raw_input": "帮我查一下山东济南的一些数据",
        "province": "山东",
        "city": "济南"
      }
    },
    {
      "action": "tool_call",
      "tool_name": "query_area_data_catalog",
      "tool_input": {
        "area_id": "370100",
        "scope_type": "city"
      }
    },
    {
      "action": "tool_call",
      "tool_name": "query_area_operational_overview",
      "tool_input": {
        "area_id": "370100",
        "time_range": "24h"
      }
    }
  ]
}
```

## 执行规则

- 同一轮最多执行 3 个 action。
- 全局仍保留最大工具调用数和知识检索数。
- 先顺序执行，不先做并发，避免工具之间依赖结果时出错。
- 每个子 action 仍生成独立事件，并带 `step`、`action_index`、`action_count` 元数据，前端按轮次聚合展示。
- 如果某个工具失败，继续执行后续不依赖它的 action，并在最终回答里说明失败原因。

## 宽泛地域查询默认链路

用户输入：“帮我查一下山东济南的一些数据”

推荐 action 链：

1. `resolve_area_scope`
2. `query_area_data_catalog`
3. `query_area_operational_overview`
4. 可选：`find_recent_anomalies`
5. 可选：`suggest_next_queries`

最终回答应包含：

- 已定位的区域。
- 当前可查的数据类型。
- 区域运行总览。
- 发现的异常或无异常说明。
- 推荐用户继续追问的方向。

## 与导出联动

如果用户说“查山东济南的数据并导出报告”，同一轮或后续轮可追加：

1. `build_evidence_pack`
2. `create_export_report`

导出计划应复用已查询到的区域、工具结果和知识证据。
