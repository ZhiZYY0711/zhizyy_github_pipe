# 06-报告导出与证据包

## 已有工具

| 工具 | 状态 | 作用 | 说明 |
| --- | --- | --- | --- |
| `create_export_report` | 已接入 | 准备导出计划，运行完成后生成文件 | 支持 PDF、Excel、TXT、Markdown、DOCX |
| `query_report_inventory` | 已接入 | 查询历史报告和导出记录 | 支持按格式和时间范围检索 |

## 已接入格式

### TXT

适用场景：

- 简单归档。
- 系统间纯文本流转。
- 复制到工单或 IM。

### Markdown

适用场景：

- 知识库沉淀。
- Git 文档归档。
- 后续转 PDF/DOCX。

### DOCX

适用场景：

- 可编辑报告。
- 领导汇报材料。
- 正式归档前人工修订。

## 已接入报告辅助工具

### `build_report_outline`

根据用户目的生成报告大纲。

输出示例：

- 用户问题。
- 数据范围。
- 关键发现。
- 风险判断。
- 处置建议。
- 证据表。
- 待补充信息。

### `generate_table_from_observations`

把工具结果变成报告表格。

表格示例：

- 区域监测概览表。
- 异常对象清单。
- 未闭环告警表。
- 任务处置状态表。

### `validate_report_completeness`

检查报告完整性。

检查项：

- 是否有明确用户问题。
- 是否有数据范围和时间范围。
- 是否有证据。
- 是否有风险判断。
- 是否有建议和待补充信息。

## 报告内容优化

导出报告不应展示：

- `recommendation_generated`
- `run_completed`
- `llm_step_started`
- `llm_step_completed`

应展示为：

- “已定位区域：山东省济南市”
- “已查询区域数据目录”
- “已查询区域运行总览”
- “已查询未闭环告警”
- “依据 SOP/案例补充处置建议”
