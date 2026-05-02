# Agent Memory v2 一期设计

日期：2026-05-02

## 背景

当前虚拟专家 Agent 已经具备轻量 summary memory 能力：用户输入包含“记住、以后、默认、下次”等触发词时，系统会把偏好写入 `memory_candidate`，后续 run 前召回并注入 ReAct prompt。这个链路已经打通，但仍偏 MVP：

- 候选记忆和已生效记忆混在 `memory_candidate` 中，语义不清。
- 只有触发词规则，没有“明显偏好表达”的提取。
- 低风险偏好和高影响偏好没有不同确认策略。
- 前端只展示 `memory_search_completed` 过程事件，用户很难明确感知 Agent 正在记忆、确认和复用偏好。
- 召回逻辑以简单 token 命中为主，第一阶段可接受，但需要更清晰的类型和注入边界。

本期 Memory v2 先聚焦用户级偏好记忆，不追求复杂业务事实记忆或案例库，而是先把“Agent 会记住我的偏好”这件事做成可见、可控、可扩展的闭环。

## 目标

1. 第一阶段只实现当前用户级偏好记忆，不做组织级共享。
2. 采用混合确认：低风险偏好自动生效，高影响偏好进入待确认。
3. 支持“显式指令 + 明显偏好表达”的偏好提取。
4. 将候选记忆和生效记忆分表：`memory_candidate` 只表示候选，新增 `user_memory` 表表示已生效偏好。
5. 前端必须清楚展示记忆功能：自动记住提示、待确认卡片、偏好列表、召回提示。
6. 保持现有 Agent 会话、run、SSE 和 Spring 代理路径兼容。

## 非目标

1. 不做业务事实、案例模式、组织知识库的自动沉淀。
2. 不引入向量化长期记忆检索。
3. 不做完整记忆管理后台。
4. 不让未经确认的高影响偏好影响 Agent 判断。
5. 不改变现有虚拟专家主 API 路径：前端仍走 `/manager/virtual-expert/agent/...`。

## 产品边界

一期记忆类型只覆盖偏好：

- `preference`：通用偏好，例如回答短一点、结论先行。
- `export_preference`：导出偏好，例如默认 Excel、带证据摘要。
- `analysis_preference`：分析口径偏好，例如先看最近 24 小时趋势。
- `interaction_preference`：交互偏好，例如不确定时先追问。

一期不把一次性业务事实写入长期记忆。例如“CP-04 今天压力波动”不是偏好，不进入 Memory v2。若用户表达“以后分析 CP-04 时优先提醒它是重点管段”，这属于带业务对象的高影响偏好，需要用户确认后才生效。

## 数据模型

### `memory_candidate`

保留现有表，语义收窄为“待处理候选”。新增字段建议：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 当前用户 ID，本期记忆只按用户隔离 |
| `candidate_type` | `preference` / `export_preference` / `analysis_preference` / `interaction_preference` |
| `preference_key` | 稳定偏好键，例如 `answer.conclusion_first` |
| `risk_level` | `low` / `high` |
| `proposed_action` | `auto_accept` / `needs_confirmation` |
| `source_text` | 触发提取的原始用户表达 |
| `reason` | 为什么识别为偏好 |

现有字段继续使用：

- `status`: `pending` / `accepted` / `rejected` / `expired`
- `confidence`
- `evidence_ids`
- `reviewed_at`
- `reviewed_by`

### `user_memory`

新增生效记忆表。召回只查这张表。

| 字段 | 说明 |
| --- | --- |
| `id` | 记忆 ID |
| `user_id` | 当前用户 ID |
| `memory_type` | 偏好类型 |
| `preference_key` | 稳定偏好键 |
| `content` | 给 LLM 注入的自然语言偏好 |
| `status` | `active` / `deleted` / `expired` |
| `risk_level` | `low` / `high` |
| `source_candidate_id` | 来源候选 ID |
| `source_session_id` | 来源会话 ID |
| `source_run_id` | 来源 run ID |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |
| `expires_at` | 可选过期时间 |

`preference_key + user_id + status=active` 用于冲突处理：同一用户同一偏好键只保留一条 active 记忆，新确认的偏好覆盖旧偏好或使旧偏好过期。

## 用户身份

Memory v2 必须按当前用户隔离。实现上由 Spring Web 代理负责取得当前登录用户，并转发给 Agent：

- 请求头：`X-Agent-User-Id`
- 本地开发或未登录 fallback：`system`

Agent 内部所有 memory candidate、user memory、召回查询都必须使用同一个 `user_id`。这个设计避免用户 A 的偏好影响用户 B。

## 偏好提取

新增 `PreferenceMemoryService.extract_candidates()`，输入为：

- `user_id`
- `session_id`
- `run_id`
- 用户消息内容
- 可选 run summary

第一阶段提取采用“规则优先，LLM 可选增强”的保守策略：

1. 显式指令：包含“记住、以后、默认、下次”等。
2. 明显偏好表达：包含“我喜欢、最好、别、不要、先、尽量、回答短一点、用表格”等。
3. 只提取偏好，不提取一次性事实。
4. 每条候选必须生成 `memory_type`、`preference_key`、`content`、`risk_level`、`reason`。

低风险偏好示例：

- “以后先给结论。”
- “回答短一点。”
- “导出时默认 Excel。”
- “不确定时先问我。”

高影响偏好示例：

- “以后风险都按高等级处理。”
- “默认忽略低级告警。”
- “报告里不要展示某类证据。”
- “以后分析某个管段时默认按重点管段处理。”

## 确认策略

### 自动接受

低风险偏好自动写入 `user_memory.active`，候选状态置为 `accepted`。同时通过 SSE 或 assistant system message 通知前端：

```json
{
  "type": "memory_accepted",
  "payload": {
    "memoryId": "mem_xxx",
    "content": "以后回答先给结论。",
    "memoryType": "preference",
    "autoAccepted": true
  }
}
```

### 待确认

高影响偏好写入 `memory_candidate.pending`，不进入 `user_memory`，不参与召回。通过 SSE 生成待确认卡片：

```json
{
  "type": "memory_candidate_created",
  "payload": {
    "candidateId": "cand_xxx",
    "content": "以后风险研判默认先看最近 24 小时趋势。",
    "riskLevel": "high",
    "reason": "该偏好会影响后续风险分析口径，需要确认。"
  }
}
```

用户点击确认后，候选转入 `user_memory.active`，候选状态置为 `accepted`。用户点击忽略后，候选状态置为 `rejected`。

## 召回与注入

每次 run 前调用 `PreferenceMemoryService.recall(user_id, query)`，最多召回 5 到 8 条 active 记忆。第一阶段使用结构化规则排序：

1. 通用输出偏好和交互偏好默认可召回。
2. 导出偏好在用户提到导出、报告、Excel、PDF 时优先。
3. 分析口径偏好在异常分析、风险研判、处置建议类问题中优先。
4. 高影响偏好只有确认后才召回。

注入 prompt 时不要再泛称“摘要记忆”，改为更清晰的分区：

```text
用户长期偏好：
- 输出偏好：回答先给结论，后给依据。
- 导出偏好：默认导出 Excel，并包含证据摘要。
- 交互偏好：信息不足时先追问用户。
```

ReAct 过程事件中同步展示召回结果：

```json
{
  "type": "memory_recalled",
  "payload": {
    "count": 2,
    "items": [
      {"memoryType": "preference", "content": "回答先给结论。"},
      {"memoryType": "export_preference", "content": "默认导出 Excel。"}
    ]
  }
}
```

## API 设计

Agent 服务新增 Memory API，Spring Web 继续做代理，前端访问 `/manager/virtual-expert/agent/...`。

建议路径：

- `GET /memories?status=active|pending`
- `POST /memory-candidates/{candidateId}/accept`
- `POST /memory-candidates/{candidateId}/reject`
- `DELETE /memories/{memoryId}`

`status=active` 返回 `user_memory` 中的生效偏好；`status=pending` 返回 `memory_candidate` 中当前用户待确认的候选。前端可以用同一个列表组件展示，但后端查询必须区分两张表，避免 pending 候选被误当作生效记忆。

响应字段使用前端友好命名：

```json
{
  "items": [
    {
      "id": "mem_xxx",
      "memoryType": "preference",
      "content": "回答先给结论。",
      "status": "active",
      "riskLevel": "low",
      "createdAt": "2026-05-02T10:00:00Z"
    }
  ]
}
```

## 前端体验

一期验收重点是让用户看见记忆功能，而不是证明记忆智能性已经足够强。

### 对话流提示

低风险自动接受时，在消息流里展示轻提示：

```text
已记住偏好：以后回答先给结论。
```

高影响候选时，在消息流里展示确认卡片：

```text
是否记住这个偏好？
以后风险研判默认先看最近 24 小时趋势。
[记住] [忽略]
```

### 过程区事件

ReAct 过程区展示以下节点：

- `偏好识别`
- `记忆候选生成`
- `已保存记忆`
- `召回偏好记忆`

### 偏好列表

虚拟专家页面提供轻量入口“我的偏好”，可以查看：

- 已生效偏好
- 待确认偏好
- 最近使用过的偏好

操作能力：

- pending：确认、忽略
- active：删除

不做完整管理后台，不做复杂筛选。

### 下一轮可见召回

如果本轮 run 召回了偏好，消息流或过程区提示：

```text
本轮已参考你的 2 条偏好：结论先行、默认导出 Excel。
```

这条提示是一期产品感的核心验收项。

## 后端模块边界

新增或调整模块：

```text
server-agent/app/services/
├── preference_memory_service.py  # 提取、确认、拒绝、删除、召回、注入格式
├── runtime_factory.py            # 构建 runtime 时注入 recalled memories
└── message_service.py            # 用户消息创建后触发偏好提取
```

`sessions.py` 只保留 API 路由和响应模型，不直接管理 Memory 细节。

`ReactRuntime` 接收 `user_preferences` 或更明确的 `preference_memory`，替代笼统的 `summary_memory` 命名。为兼容测试，可以先保留旧参数并逐步迁移。

## 兼容策略

1. 保留现有 `summary_memory` 行为，新增偏好记忆注入时先兼容旧字段。
2. 现有 `memory_candidate` 中已 accepted 的偏好可在迁移时转入 `user_memory`。
3. 如果没有登录用户，使用 `system` 作为本地开发用户。
4. 如果 LLM 不可用，规则提取仍可工作。
5. 如果前端暂未接入 Memory API，自动接受提示仍可通过 SSE 事件显示在过程区。

## 测试策略

后端测试：

- 显式“记住”偏好会生成候选。
- 明显偏好表达会生成候选。
- 一次性业务事实不会生成偏好。
- 低风险偏好自动进入 `user_memory.active`。
- 高影响偏好进入 `memory_candidate.pending`。
- accept 会创建 active memory，并将 candidate 置为 accepted。
- reject 会将 candidate 置为 rejected，不参与召回。
- delete 会将 active memory 置为 deleted，不参与召回。
- run 前只召回当前用户 active memory。
- 用户 A 的偏好不会被用户 B 召回。
- 召回事件 `memory_recalled` 会进入 SSE。

前端测试：

- 自动记住事件展示轻提示。
- pending 候选展示确认卡片。
- 点击确认调用 accept API 并更新列表。
- 点击忽略调用 reject API 并移除卡片。
- “我的偏好”列表能展示 active 和 pending。
- 新 run 召回偏好时展示“本轮已参考”提示。

推荐验证命令：

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py tests/test_react_runtime.py tests/test_persistence_models.py -q

cd frontend
pnpm exec vitest run
pnpm build
```

如 Spring 代理新增接口，再运行：

```bash
cd server
mvn -pl server-web -am test-compile
```

## 验收标准

1. 用户表达低风险偏好后，前端能看到“已记住偏好”。
2. 用户表达高影响偏好后，前端能看到确认卡片，并能确认或忽略。
3. 已确认偏好能在“我的偏好”轻列表中看到。
4. 下一轮 run 能展示“本轮已参考你的偏好”。
5. 只有当前用户的 active memory 会参与召回。
6. 未确认 pending memory 不影响 Agent 输出。
7. 现有虚拟专家会话、run、SSE、导出接口不发生破坏性变化。
