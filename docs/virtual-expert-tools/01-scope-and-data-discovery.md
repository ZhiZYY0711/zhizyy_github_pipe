# 01-范围解析与数据发现

## 已有工具

| 工具 | 状态 | 作用 | 说明 |
| --- | --- | --- | --- |
| `resolve_operational_scope` | 已接入 | 将用户描述的区域、管线、管段、设备转换为候选对象 | 适合对象候选发现 |
| `query_report_inventory` | 已接入 | 查询历史报告、导出记录、附件清单 | 偏报告目录，不解决业务数据发现 |

## 已接入工具

### `resolve_area_scope`

将自然语言地名解析为系统区域。

输入建议：

```json
{
  "raw_input": "帮我查一下山东济南的一些数据",
  "province": "山东",
  "city": "济南",
  "district": ""
}
```

输出建议：

```json
{
  "summary": "已定位到山东省济南市",
  "records": [
    {
      "scope_type": "city",
      "province_code": "370000",
      "province_name": "山东省",
      "city_code": "370100",
      "city_name": "济南市",
      "area_id": "370100"
    }
  ],
  "context": {
    "resolved_scope_id": "370100",
    "resolved_scope_type": "city"
  }
}
```

实现来源：

- Java 侧复用 `AreaService` / `AreaMapper` 的省市区列表。
- 支持模糊匹配：`山东` 匹配 `山东省`，`济南` 匹配 `济南市`。

### `query_area_data_catalog`

查询区域下有哪些业务数据可用。

输入建议：

```json
{
  "area_id": "370100",
  "scope_type": "city",
  "include_samples": true
}
```

输出建议：

- 可查管线数量、管段数量、设备数量。
- 可查监测指标：压力、流量、温度、振动。
- 告警数据是否存在、最近告警数量。
- 巡检、维修、任务数据是否存在。
- 返回 5 到 10 条代表性管段或设备候选。

使用场景：

- 用户只说“查一下某地数据”。
- 用户不清楚系统里有哪些数据。
- Agent 需要先给用户一个可选菜单。

## 已接入辅助能力

### `suggest_next_queries`

根据数据目录和当前问题生成下一步查询建议。

输出示例：

- “查看济南市近 24 小时告警”
- “查看济南市压力最高的管段”
- “查看济南市未闭环巡检任务”
- “导出济南市运行概览报告”

## Prompt 策略

用户给出省、市、区但没有指标时，优先调用：

1. `resolve_area_scope`
2. `query_area_data_catalog`
3. `query_area_operational_overview`

只有当区域也无法解析时，才向用户追问。
