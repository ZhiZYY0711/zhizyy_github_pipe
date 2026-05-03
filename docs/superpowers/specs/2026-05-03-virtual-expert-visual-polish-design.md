# 虚拟专家页面视觉精细打磨设计

## 背景

虚拟专家页面（VirtualExpertPage.vue）当前功能完整，但侧边栏和输入区的视觉细节不够精致：工具栏按钮平铺无层次、会话卡片交互反馈生硬、输入区 textarea 周围按钮散乱、整体缺少工业风的"嵌入式控制面板"质感。本次优化目标是在不改变功能逻辑的前提下，对侧边栏和输入区进行全面的视觉精细打磨，严格遵循 `docs/DESIGN.md` 定义的 Tactical Brutalism 设计系统。

## 目标

第一，侧边栏工具栏改为垂直图标+文字按钮列，每个按钮独占一行，提供更清晰的功能分区和交互反馈。

第二，搜索交互改为工具栏按钮触发内联搜索框展开，保持工业风的嵌入式理念。

第三，会话卡片、菜单、归档区域的交互状态全部按设计系统规范打磨：0px 圆角、色调分层、机械过渡。

第四，输入区重组为全宽 textarea + 底部图标工具栏，外层包裹一个更高 surface 层级的容器，形成"命令输入面板"质感。

第五，所有按钮替换为内联 SVG 图标，消除文字按钮的"AI模板感"。

## 非目标

本次不改变任何功能逻辑、API 调用、数据流或路由。不改变对话区域（气泡、时间线、证据卡片）的样式。不引入新的 UI 框架、图标库或 CSS 预处理器。不改变响应式断点逻辑。

## 设计系统约束

所有视觉决策必须遵循 `docs/DESIGN.md` 中的 Tactical Brutalism 规范：

- **0px 圆角**：所有 `border-radius` 为 `0px`，不允许圆角
- **机械过渡**：`transition` 使用 `80ms step-end`，不允许 ease/bounce/spring
- **色调分层**：通过 surface 层级区分高度，不用 drop-shadow 做结构区分
- **Ghost 边框**：使用 `outline-variant` 20% 透明度，不用 1px solid 实线
- **凹陷输入**：输入框使用 `surface-container-lowest` 背景
- **禁止纯灰**：使用 slate blue 和 charcoal 色调，禁止 #808080
- **克制用色**：`safety orange` 和 `glowing cyan` 仅用于关键引导

## 设计令牌映射

本设计使用的 surface 层级（对应 tokens.css 现有变量）：

| 设计系统层级 | tokens.css 变量 | 用途 |
|---|---|---|
| `surface` / `background` | `--color-bg` (#04080d) | 页面基底 |
| `surface-container-low` | `--color-panel` (#0b131b) | 对话区背景 |
| `surface-container-high` | `--color-panel-2` (#101922) | 输入区容器背景 |
| `surface-bright` | `--color-panel-3` (#15202b) | hover 态、菜单背景 |
| `surface-container-lowest` | `--color-bg-elevated` (#070d13) | 输入框凹陷背景 |
| `outline-variant` | `--color-line` (rgba(143,166,182,0.14)) | Ghost 边框 |
| `secondary` (Cyan) | `--color-accent-cyan` (#6ecad4) | Focus 态、高亮 |
| `primary_container` (Orange) | `--color-accent-orange` (#e7682d) | 发送按钮、active 指示 |
| `tertiary` (Hazard Yellow) | `--color-warning` (#ddb054) | 停止按钮 |

## 侧边栏设计

### 工具栏重构

当前工具栏是水平排列的文字按钮。改为垂直图标+文字按钮列：

```text
[◨ 收起列表]
[+ 新对话]
[🔍 搜索]
[📦 归档]
[⚙ 偏好]
```

每个按钮规格：
- 高度 `36px`，独占一行
- 左侧 `12px` 宽图标区（内联 SVG），右侧文字 `--text-meta`（12px）
- 默认态：背景透明，ghost 边框（`--color-line`）
- hover 态：背景 `--color-panel-3`（surface-bright），边框 `rgba(110, 202, 212, 0.2)`
- active 态：背景 `rgba(110, 202, 212, 0.12)`
- 过渡：`background 80ms step-end, border-color 80ms step-end`
- 0px 圆角

按钮之间间距 `2px`。

### 搜索交互

搜索按钮点击后，在工具栏下方展开内联搜索输入框（不是模态框）：

```text
[◨ 收起列表]
[+ 新对话]
[🔍 搜索]          ← 点击后展开下方输入框
┌─────────────────┐
│ 搜索历史记录      │  ← surface-container-lowest 凹陷
└─────────────────┘
[📦 归档]
[⚙ 偏好]
```

搜索框规格：
- 背景 `--color-bg-elevated`（surface-container-lowest），0px 圆角
- 默认边框 `--color-line`
- focus 态：边框变为 `--color-accent-cyan` 1px solid + `box-shadow: 0 0 6px rgba(110, 202, 212, 0.25)`
- 高度 `32px`，字号 `--text-meta`
- 过渡 `80ms step-end`

再次点击搜索按钮或按 Esc 收起搜索框。

### 会话卡片

- 0px 圆角
- 间距从 `4px` 调整为 `8px`（设计系统："用 8px 间距分隔列表项，不要 1px 分隔线"）
- 默认态：背景 `rgba(4, 9, 14, 0.8)`，边框 `--color-line`
- hover 态：背景切到 `--color-panel-3`（surface-bright），边框 `rgba(110, 202, 212, 0.2)`，**无 translateY**
- active 态：左侧 `2px solid --color-accent-orange` 色条，背景切到 `--color-panel-2`（surface-container-high）
- 过渡：`background 80ms step-end, border-color 80ms step-end, border-left 80ms step-end`
- 标题字号 `--text-meta`，`letter-spacing: 0.02em`

### "更多"按钮与菜单

"更多"按钮：
- 默认 `opacity: 0`，hover 卡片时 `opacity: 1`，过渡 `80ms step-end`
- 0px 圆角，ghost 边框
- hover 态背景 `--color-panel-3`

菜单弹出层：
- 0px 圆角，背景 `--color-panel-3`（surface-bright）
- 阴影：`0 20px 40px rgba(0, 0, 0, 0.4)`（设计系统允许的环境阴影）
- 菜单项之间用 `8px` 间距分隔，不用分隔线
- 菜单项 hover：背景切到 `--color-panel-2`
- 过渡 `80ms step-end`

### 归档区域

- 归档 toggle 按钮：0px 圆角，ghost 边框，与工具栏按钮风格一致
- 归档列表项：复用会话卡片样式（0px 圆角、8px 间距、相同 hover/active 态）

## 输入区设计

### 容器结构

输入区重组为嵌套容器结构，形成"命令输入面板"质感：

```text
┌─────────────────────────────────────────────────────┐
│  外层容器: --color-panel-2 (surface-container-high)  │
│  顶部 ghost 边框 (--color-line)                      │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │  [附件预览 chips]                               │  │
│  │                                                │  │
│  │  ┌─────────────────────────────────────────┐  │  │
│  │  │  textarea                                │  │  │
│  │  │  --color-bg-elevated (凹陷)              │  │  │
│  │  │  0px 圆角                                │  │  │
│  │  └─────────────────────────────────────────┘  │  │
│  │                                                │  │
│  │  [📎] [🎤] [🖼] [📤] [模型▼]    [⏹ 停止] [▶ 发送] │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

三层 surface 嵌套：
1. 对话区：`--color-panel`（surface-container-low）
2. 输入容器：`--color-panel-2`（surface-container-high）
3. 输入框：`--color-bg-elevated`（surface-container-lowest）

### Textarea

- 全宽，取消右侧 128px 操作列
- 背景 `--color-bg-elevated`（surface-container-lowest），0px 圆角
- 默认边框 `--color-line`
- focus 态：边框变为 `--color-accent-cyan` 1px solid + `box-shadow: 0 0 6px rgba(110, 202, 212, 0.25)`
- 最小高度 `78px`，最大高度 `128px`，可拖拽调整
- 过渡 `80ms step-end`

### 底部工具栏

全图标按钮，无文字。hover 时通过 tooltip 显示中文名。

左侧功能按钮组：

| 按钮 | 图标关键词（搜索用） | 说明 |
|------|---------------------|------|
| 附件 | paperclip icon SVG | 添加图片附件 |
| 语音 | microphone icon SVG | 语音输入 |
| 图片 | image icon SVG | 上传图片 |
| 导出 | download icon SVG | 导出对话 |
| 模型 | sliders icon SVG | 模型挡位选择 |

右侧操作按钮组：

| 按钮 | 图标关键词（搜索用） | 说明 |
|------|---------------------|------|
| 停止 | square icon SVG | 停止当前运行（运行中显示） |
| 发送 | arrow-up icon SVG | 发送消息（空闲时显示） |

按钮规格：
- 高度 `32px`，最小宽度 `32px`，0px 圆角
- 功能按钮：ghost 风格，边框 `--color-line`，hover 背景 `--color-panel-3`
- 发送按钮：primary solid，背景 `--color-accent-orange`，hover 带 `box-shadow: 0 0 8px rgba(231, 104, 45, 0.4)`
- 停止按钮：hazard 风格，边框 `rgba(221, 176, 84, 0.34)`，背景 `rgba(221, 176, 84, 0.08)`，带 pulse 动画（opacity 0.8→1.0，1.2s infinite）
- 过渡 `80ms step-end`
- 按钮间距 `4px`
- 左右两组之间用 `margin-left: auto` 推开

### 模型选择器

- 凹陷风格，与输入框一致：背景 `--color-bg-elevated`，0px 圆角
- 显示当前挡位缩写（Auto/轻量/标准/性能/极致）
- 点击展开选择面板：0px 圆角，背景 `--color-panel-3`，环境阴影
- 选项 hover 背景 `--color-panel-2`
- 当前选中项左侧 `2px` 橙色指示条

### 附件预览

- 附件 chip 保持现有布局，0px 圆角
- chip 背景 `--color-bg-elevated`，边框 `--color-line`
- 删除按钮：hazard 风格

## 图标规范

所有图标使用内联 SVG，统一规格：
- 尺寸 `16px × 16px`
- 描边风格（stroke），`stroke-width: 1.5` 或 `2`
- 颜色 `currentColor`，跟随按钮文字颜色
- 不引入外部图标库，直接在组件模板中内联 SVG

推荐图标来源（参考，手动提取 SVG path）：
- Lucide Icons (lucide.dev) — MIT 协议，线条风格
- Heroicons (heroicons.com) — MIT 协议
- Phosphor Icons (phosphoricons.com) — MIT 协议

## 实现范围

仅修改 `VirtualExpertPage.vue` 一个文件，变更范围：

1. **Template**：工具栏按钮改为图标+文字垂直列；搜索改为按钮触发展开；输入区重组为容器嵌套结构；所有文字按钮替换为图标按钮
2. **Script**：新增 `searchExpanded` 状态；工具栏按钮数据改为带图标的数组
3. **Style**：按上述规范调整所有侧边栏和输入区 CSS 规则

不修改 `VirtualExpertSharedPage.vue`、`tokens.css`、`business.css` 或其他文件。

## 测试计划

1. `pnpm build` 无报错
2. `pnpm test` 现有测试通过
3. 视觉检查：侧边栏工具栏垂直排列、图标显示正确、hover/active 状态符合设计系统
4. 视觉检查：搜索框展开/收起正常、focus 态有 cyan 光晕
5. 视觉检查：会话卡片 0px 圆角、8px 间距、hover 背景色调分层、active 左侧橙色条
6. 视觉检查：输入区三层 surface 嵌套、textarea 凹陷、工具栏图标对齐
7. 视觉检查：发送按钮 primary solid、停止按钮 hazard pulse、模型选择器凹陷风格
8. 交互检查：所有过渡为机械感 step-end，无 ease/bounce
9. 响应式检查：1180px 和 819px 断点下布局正常
