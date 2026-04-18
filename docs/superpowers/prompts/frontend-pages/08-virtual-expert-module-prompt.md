# 虚拟专家模块生成提示词

设计一个【虚拟专家工作台】，用于【就监测异常、风险判断和处置建议进行咨询，实现 SSE 流式对话、会话管理与历史记录查看的专家会商工作台】。

==================================================

【一、页面定位（Page Role）】

- 页面类型：双栏工作台（专家会商台）
- 用户角色：需要就监测异常、风险判断和处置建议进行咨询的业务人员
- 核心目标：
  - 目标1：SSE 流式对话（实时专家咨询）
  - 目标2：会话管理（历史会话、新建、关闭）
  - 目标3：保持独立模块地位，与普通业务模块形成明显识别差异
- 使用场景：业务人员遇到监测异常需专家判断，或需要处置建议咨询

------

【二、整体布局（Layout System）】

- 顶部结构：固定主导航（虚拟专家需有醒目标识，与普通模块明显区分）
- 主体布局：左右双栏
- 区域划分：
  - 左侧：历史会话与连接状态区
  - 右侧：当前对话主工作区
- 是否使用特殊布局：双栏工作台

------

【三、信息架构（Information Architecture）】

模块1：历史会话与连接状态区（左侧）

- 功能说明：会话管理与连接状态
- 包含信息：
  - 历史会话列表（卡片式，易扫描）
  - 新建对话入口
  - 当前 SSE 连接状态（状态灯）
  - 会话筛选或快速定位
- 优先级：高

模块2：当前对话主工作区（右侧）

- 功能说明：专家对话核心交互区
- 包含信息：
  - 专家身份头部
  - 对话消息流
  - 流式回复中的打字状态
  - 消息输入与发送
  - 会话级操作（清空、关闭、查看会话记录）
- 优先级：最高

------

【四、核心模块详细设计（Key Module Design）】

模块名称：当前对话主工作区

- 展示形式：对话消息流
- 每个单元包含：
  - 消息气泡（用户/专家区分）
  - 流式回复逐字生成效果
  - 处理中/失败重试提示
- 操作区：
  - 主操作：发送消息（POST /manager/virtual-expert/messages）
  - 次操作：关闭会话（POST /manager/virtual-expert/conversations/{conversationId}/close）
- 状态设计：
  - SSE 连接中：secondary 青色状态灯
  - 流式回复中：primary 橙色打字指示
  - 连接断开：tertiary 危险黄色脉冲
  - 回复失败：error 红 + 重试提示

模块名称：历史会话与连接状态区

- 展示形式：卡片列表
- 每个单元包含：
  - 会话标题/摘要
  - 时间戳（Space Grotesk 等宽）
  - 会话状态标识
- 操作区：
  - 主操作：点击会话切换
  - 次操作：新建对话、筛选
- 状态设计：
  - SSE 在线：secondary 青色状态灯（醒目但克制）
  - SSE 离线：tertiary 危险黄色脉冲状态灯

------

【五、交互模型（Interaction Model）】

- 主交互方式：消息输入 + 流式回复
- 分层交互：
  - 第一层：历史会话浏览与切换
  - 第二层：当前对话消息流交互
  - 第三层：会话级操作（关闭、清空、查看记录）
- 页面跳转策略：不跳转，全部在工作台内完成
- 动态行为：SSE 流式逐字生成，连接状态实时更新，快捷提问 chips 点击即发送

------

【六、视觉与层级（Visual Hierarchy）】

- 视觉优先级：
  1. 当前对话主工作区（核心交互）
  2. SSE 连接状态（连接可靠性）
  3. 历史会话列表（会话管理）
- 设计风格：Tactical Brutalism — 控制系统中的"专家会商台"：冷硬、可信、安静、有明显的工作台属性
- 组件风格：
  - 消息气泡：tonal recession 分区（用户 surface-container-high / 专家 surface-container-low）
  - 状态灯：secondary 青色 / tertiary 危险黄色，醒目但克制
  - 快捷提问 chips：secondary_container 青色 Data Chip
  - 输入框：Recessed 风格（surface-container-lowest 背景，focus 时 secondary 青色发光边框）
- 状态颜色规范：
  - SSE 在线：secondary 青色
  - 流式回复中：primary 橙色
  - 连接断开：tertiary 危险黄色脉冲
  - 回复失败：error 红

------

【七、动效设计（Motion & Feedback）】

- Hover 动效：历史会话卡片背景偏移至 surface-bright
- 切换动画：会话切换 Instant（0ms）
- 加载动画：流式回复逐字生成（Mechanical Step-easing），CRT 扫描线纹理
- 操作反馈：连接状态实时更新，失败重试提示

------

【八、扩展能力（Advanced Features，可选）】

- 快捷提问 chips（常见问题入口）
- 历史会话按时间/关键词前端筛选
- 流式回复逐字生成、处理中、失败重试提示
- SSE 连接状态醒目状态灯

------

【九、设计目标（Design Goals）】

- 像控制系统中的"专家会商台"，不是通用 AI 网站，也不是营销聊天机器人
- 冷硬、可信、安静、有明显的工作台属性
- 保持独立模块地位，与普通业务模块形成明显识别差异
- 流式对话体验流畅，连接状态清晰

------

【十、限制与规范（Constraints）】

- 业务来源：docs/api/07-虚拟专家模块.md + backstage-vue/src/modules/emergency/views/VirtualExpert.vue
- 页面能力严格限制在现有接口：
  - GET /manager/virtual-expert/stream（SSE 流式连接）
  - POST /manager/virtual-expert/messages（发送消息）
  - POST /manager/virtual-expert/conversations/{conversationId}/close（关闭会话）
  - GET /manager/chat-logs（获取聊天记录列表）
  - GET /manager/chat-logs/{fileName}（获取聊天记录内容）
  - GET /status（获取 SSE 服务状态）
- 明确禁止：
  - 不新增知识库上传
  - 不新增多专家协同编排
  - 不新增模型参数面板
  - 不新增后端不存在的推荐、诊断、工单联动写入能力
  - 不把虚拟专家做成普通 CRUD 页
- 展示可以升级，但业务能力不能越界
- 不使用圆角（borderRadius: 0px）
- 不使用纯灰色（#808080）

==================================================

请一定先阅读 docs/api
