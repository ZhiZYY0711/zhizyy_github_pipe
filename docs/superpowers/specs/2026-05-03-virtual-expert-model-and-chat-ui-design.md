# 虚拟专家模型挡位与对话体验优化设计

## 背景

虚拟专家当前只通过全局 `AGENT_LLM_MODEL` 决定模型，前端会话列表和输入区仍偏工程化：列表卡片展示了时间、来源、摘要和独立删除按钮，信息密度低；输入区包含预设提示词和多个文字按钮，操作区不够紧凑。用户希望新增五个模型挡位，并把模型切换限定为“下一次用户输入生效”，同时重做左侧会话管理、标题生成、分享和输入区交互。

本设计只定义实现方案，不直接改业务代码。后续实现应保持现有设计系统变量、面板风格和 ReAct 时间线结构，不引入新的 UI 框架或图标库；图标优先使用本项目内联 SVG 组件或轻量本地 icon helper。

## 目标

第一，模型选择提供五个清晰挡位：Auto、轻量、标准、性能、极致。Auto 直接调用官方 `moonshot-v1-auto`，不做本地智能路由。用户在一次 ReAct 运行中切换模型时，只影响下一次用户输入，不影响当前 run。

第二，左侧列表改成可展开和隐藏的会话导航。删除“研判对话”标题，压缩“开启新对话”和“我的偏好”的尺寸与间距，新增搜索历史记录和归档聊天入口。历史记录只展示标题，移除时间、内容摘要和现有删除按钮。

第三，会话标题由低成本模型自动总结生成，支持菜单操作：重命名、删除、归档、置顶聊天、分享。分享支持生成 Markdown 文件或生成页面链接。Markdown 分享必须包含用户问题、ReAct 过程线、工具调用摘要、关键证据和最终答案，不能只导出问题与最终答案。本人访问会话链接进入正常 Web 端；他人访问分享链接进入同设计风格的只读分享页。

第四，输入区参考提供的两张图，保留当前系统的暗色工业风设计变量，但重新组织语音、图片、导出、发送、停止、模型选择等操作。预设提示词删除，能用图标的操作使用图标按钮。

## 非目标

本期不做任意自定义模型字符串输入，只开放五个白名单挡位。本期不引入第三方图标库。本期不做公开匿名编辑、评论、多人协作，也不允许分享页触发新的 Agent run。本期不改变 ReAct 工具调用策略，只改变每个 run 使用的 LLM client 初始化模型。

## 模型挡位

模型挡位后端统一白名单解析，前端只传 `modelTier`。解析结果写入 run，整个 run 生命周期固定不变。

| 挡位 | modelTier | resolvedModel | 展示名 | 说明 |
| --- | --- | --- | --- | --- |
| Auto | `auto` | `moonshot-v1-auto` | Auto | 官方自动模式，默认推荐 |
| 轻量 | `light` | `moonshot-v1-8k` | 轻量 | 简单问答、短摘要、低成本 |
| 标准 | `standard` | `moonshot-v1-32k` | 标准 | 日常研判、普通工具调用 |
| 性能 | `performance` | `kimi-k2.5` | 性能 | 较复杂 Agent、长上下文、多模态 |
| 极致 | `ultimate` | `kimi-k2.6` | 极致 | 最强推理、长程 ReAct、复杂报告 |

默认值配置为 `auto`。`.env.example` 新增：

```env
AGENT_LLM_DEFAULT_TIER=auto
AGENT_LLM_AUTO_MODEL=moonshot-v1-auto
AGENT_LLM_LIGHT_MODEL=moonshot-v1-8k
AGENT_LLM_STANDARD_MODEL=moonshot-v1-32k
AGENT_LLM_PERFORMANCE_MODEL=kimi-k2.5
AGENT_LLM_ULTIMATE_MODEL=kimi-k2.6
AGENT_LLM_TITLE_MODEL=moonshot-v1-8k
```

现有 `AGENT_LLM_MODEL` 保留作为兼容回退：如果没有传 tier 或 tier 解析失败，使用 `AGENT_LLM_DEFAULT_TIER`；如果挡位模型为空，再回退 `AGENT_LLM_MODEL`。

## 后端设计

### 配置与 LLM Client

`Settings` 增加五挡模型配置和标题模型配置。`build_llm_client()` 增加 `model_override` 参数，保持默认行为不变。新增 `resolve_model_choice(model_tier: str | None) -> ModelChoice`，返回 `tier`、`provider`、`model`、`label`。

`AgentRuntimeService` 接收 `llm_model` 或 `model_choice`，创建 `ReactRuntime` 时使用该模型。这样同一 run 内所有 ReAct step 共用同一个模型，避免中途切换导致 prompt cache 命中率下降。

### Run 固化模型

`CreateMessageRequest` 增加 `model_tier: str | None`。`CreateSessionRequest` 也支持相同字段，用于后续首轮直接运行场景。`repository.create_run()` 增加 `model_provider`、`model_name`、`model_tier` 入参。

数据库当前 `agent_run` 已有 `model_provider`、`model_name`，建议新增 `model_tier varchar(32)`。如果迁移成本需要分阶段处理，第一阶段可把 `model_tier` 写入 `state_snapshot` 或 run start event payload，但正式落库应加字段。

运行开始前发 `model_selected` 事件：

```json
{
  "type": "model_selected",
  "title": "本轮模型已锁定",
  "payload": {
    "tier": "performance",
    "label": "性能",
    "provider": "moonshot",
    "model": "kimi-k2.5"
  }
}
```

前端显示该事件，但不进入普通工具时间线噪音区。

### 标题生成

新增低成本标题生成服务，触发点为“第一条用户消息成功创建且 session 还处于默认标题”。使用 `AGENT_LLM_TITLE_MODEL`，推荐 `moonshot-v1-8k`。

标题 prompt 要求中文、8 到 18 字、不要标点堆叠、不要包含“请问/帮我/分析一下”等泛词。标题生成失败时回退原有 `sessionTitle = input.slice(0, 36)`。标题生成不阻塞 run，可以异步完成后更新 session title，并推送或由前端列表刷新感知。自动标题一旦生成后不允许用户关闭该能力，但用户可以通过重命名覆盖具体标题。

需要新增 repository 方法：`update_session_title(session_id, title)`。

### 会话操作 API

新增或扩展接口：

```text
PATCH /api/agent/sessions/{session_id}
body: { title?, archived?, pinned? }

POST /api/agent/sessions/{session_id}/share
body: { type: "link" | "md" }

GET /api/agent/shared/{share_id}
```

`PATCH` 负责重命名、归档、取消归档、置顶和取消置顶。删除沿用现有 DELETE，但前端从列表卡片上移除独立删除按钮，放到菜单中。

分享链接返回 `shareId`、`shareUrl`、`expiresAt?`、`markdownDownloadUrl?`。Markdown 分享可以复用现有导出能力的 `md` 格式，但入口从会话菜单发起，默认范围为当前会话。Markdown 内容必须包含完整 ReAct 过程线：每轮思考摘要、工具批次、工具名称、工具结果摘要、关键证据、风险判断、最终答案。内部原始 payload、权限、token 和未确认 memory 不写入 Markdown。

### 分享数据模型

建议新增 `agent_share` 表：

```text
id varchar(64) primary key
session_id varchar(64)
created_by varchar(64)
share_type varchar(16)
title varchar(200)
snapshot jsonb
created_at timestamptz
expires_at timestamptz null
revoked_at timestamptz null
```

分享页读取 snapshot，避免原会话后续变化影响已分享内容。snapshot 包含 session、messages、run summaries、ReAct 过程线、关键 events 和 final answer，不包含内部 token、权限、用户偏好详情和未确认 memory。

## 前端设计

### 路由

当前自研 router 只支持精确路径匹配，需要支持参数化路径。建议先扩展最小能力：

```text
/virtual-expert
/virtual-expert/:sessionId
/virtual-expert/share/:shareId
```

本人打开 `/virtual-expert/:sessionId` 后，页面自动选中该 session 并加载 timeline。分享页 `/virtual-expert/share/:shareId` 使用新的只读页面组件 `VirtualExpertSharedPage.vue`，复用时间线展示组件和 Markdown 渲染，但不显示输入区、记忆、导出操作和内部工具详情中的敏感字段。

### 左侧会话导航

左侧改成三种状态：

1. 展开：显示顶部图标操作、新建、搜索、偏好、归档入口和标题列表。
2. 收起：显示一列图标按钮，包括展开、新建、搜索、偏好、归档。
3. 隐藏：主内容占满宽度，左侧只保留一个窄浮动展开按钮。

删除“研判对话”标题。顶部建议只保留紧凑工具栏：

```text
[折叠] [新建] [搜索] [归档] [偏好]
```

“开启新对话”和“我的偏好”从大块按钮改为 30 到 34px 高的 icon + text 小按钮。历史记录 item 只展示标题，字号使用 `var(--text-meta)` 或更小一档，单行省略。置顶会话显示一个 pin 图标，不额外展示时间。归档聊天在左侧视图内呈现，默认折叠；点击“归档聊天”后展开归档分组，不打开单独弹层。

搜索历史记录在左侧展开态下显示为紧凑输入框；收起态点击搜索图标打开浮层。搜索只匹配本地已加载 session title，后续可扩展后端 query。

每条会话右侧 hover 或 active 时显示更多菜单按钮。菜单项为：重命名、置顶/取消置顶、归档/取消归档、分享、删除。删除需要确认。

### 分享交互

点击分享打开弹层，提供两个选项：

1. 生成本页链接：创建 share record，复制 `/virtual-expert/share/{shareId}`。
2. 生成 Markdown：调用分享或导出接口生成 `.md`，显示下载链接。

本人会话链接不是分享链接，格式为 `/virtual-expert/{sessionId}`。它需要登录，进入完整 Web 端。分享链接是完全公开访问的只读页面，格式为 `/virtual-expert/share/{shareId}`。公开接口只能读取分享 snapshot，不能读取原始会话、用户偏好、未确认记忆、内部权限或任意导出资源。

### 输入区

删除预设提示词区域。输入区改为单个紧凑 composer：

```text
[+] [textarea/input] [模型挡位] [导出] [语音] [图片] [停止/发送]
```

`+` 用于展开更多操作，包含图片、导出、清空附件等。模型挡位是小型下拉或 segmented popover，展示 Auto/轻量/标准/性能/极致。语音、图片、导出、发送、停止使用图标按钮，hover tooltip 显示中文名称。运行中发送按钮切换为停止按钮；如果用户切换模型，composer 上方或模型按钮 tooltip 显示“将在下一次提问生效”。

输入框仍支持 Enter 发送、Shift+Enter 换行。图片附件保持现有最多 4 张限制，以缩略 chip 横向展示在输入框上沿。

### 模型选择状态

前端维护：

```ts
selectedModelTier: ModelTier
activeRunModelTier?: ModelTier
```

发送消息时把 `selectedModelTier` 快照进请求。运行中用户改 `selectedModelTier` 不改当前 run，只改变下一次请求。收到 `model_selected` 事件后更新当前 run 展示。

### 分享页视觉

分享页使用与虚拟专家工作台一致的面板、字体、颜色和时间线样式，但布局更简单：

```text
顶部：标题、来源标识、分享时间
主体：用户问题、最终回答、关键证据、必要时间线
底部：只读提示
```

分享页不展示“我的偏好”、未确认记忆、内部权限、完整工具 payload 和操作按钮。工具结果只展示已归纳的 evidence card，并保留 ReAct 过程线的轮次、工具批次和关键证据脉络。

## 数据迁移

建议新增字段：

```sql
ALTER TABLE agent_run ADD COLUMN model_tier varchar(32);
ALTER TABLE analysis_session ADD COLUMN pinned boolean DEFAULT false NOT NULL;
ALTER TABLE analysis_session ADD COLUMN archived_at timestamptz;
CREATE TABLE agent_share (...);
```

如果项目当前没有 migration 框架，应新增 SQL 脚本到 `database/postgres/02-migrations/`，并在 README 或部署说明中记录执行顺序。

## 测试计划

后端测试：

1. `resolve_model_choice("auto")` 返回 `moonshot-v1-auto`。
2. 五个合法 tier 都能解析，非法 tier 回退默认或返回 400，按最终实现统一。
3. 创建 run 时写入 `model_tier`、`model_provider`、`model_name`。
4. ReAct runtime 使用 run 固化模型，运行中不会读取后续前端选择。
5. 标题生成成功时更新 session title，失败时保留回退标题。
6. PATCH session 支持重命名、归档、置顶。
7. 分享接口不返回未确认 memory 和内部敏感字段。
8. Markdown 分享包含 ReAct 过程线、工具调用摘要、关键证据和最终答案。

前端测试：

1. 模型选择发送时随消息提交，运行中切换只影响下一次提交。
2. 左侧列表只展示标题，删除按钮不再出现在 item 固定位置。
3. 会话菜单能触发重命名、删除、归档、置顶和分享。
4. `/virtual-expert/:sessionId` 能进入并选中指定会话。
5. `/virtual-expert/share/:shareId` 渲染只读分享页。
6. 输入区无预设提示词，语音、图片、导出、发送、停止均为图标按钮且 disabled 状态正确。
7. 归档聊天位于左侧视图内，默认折叠，展开后只展示归档会话标题。

验证命令：

```bash
cd server-agent && PYTHONPATH=. uv run pytest tests/test_sessions_api.py tests/test_tool_registry.py tests/test_react_runtime.py -q
cd frontend && pnpm build
cd server && mvn -pl server-web -Dtest=VirtualExpertToolProxyControllerTest test
```

## 实现顺序

第一步实现模型挡位后端解析、`.env.example`、run 固化和 `model_selected` 事件，再接前端模型选择器。第二步实现标题生成和 session 更新 API。第三步重构左侧列表、搜索、归档、菜单操作。第四步实现路由参数、本人会话路径和分享页。第五步重组输入区并删除预设提示词。第六步补测试、构建和视觉检查。

## 已定决策

分享页完全公开访问，但只能访问分享 snapshot，不能透传原始会话权限。归档聊天放在左侧视图内，默认折叠，不做单独弹层。自动标题能力不提供关闭入口，用户需要调整标题时通过重命名覆盖。
