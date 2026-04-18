# Frontend Login And Auth Entry Design

## 1. 目标

本 spec 只解决一件事：把 `frontend` 里的登录页从静态样板改成真实可用的认证入口，并为后续页面迁移提供最小可用的鉴权基础设施。

本轮目标包括：

- 登录页发起真实网络请求
- Vite 开发环境通过 `/api` 代理转发到 `http://localhost:8080`
- 登录成功后写入标准登录态
- 新前端具备最小路由和鉴权守卫能力
- 登录完成后能进入新的 Dashboard 壳

本轮明确不做：

- 完整用户信息页
- 刷新 token 机制
- 权限点细粒度控制
- 真实退出接口接线
- Dashboard 全量真实数据接入

## 2. 已确认契约

### 2.1 接口路径

真实登录接口固定为：

- `POST /api/manager/login`

开发环境代理规则固定为：

- 浏览器访问 `/api/*`
- Vite 代理转发到 `http://localhost:8080`

### 2.2 请求体

请求字段沿用旧系统约定：

```json
{
  "username": "admin729",
  "password": "0192023a7bbd73250516f069df18b500",
  "code": "46h2s"
}
```

字段含义：

- `username`: 用户名
- `password`: 前端 MD5 后的密码字符串
- `code`: 前端验证码输入值

### 2.3 响应体

以旧项目中的接口文档为准，响应结构按如下解析：

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "username": "string",
    "jwt": "string"
  }
}
```

说明：

- 旧文档中成功码为 `0`
- 旧 Vue 2 页面里有一处按 `response.code === 200` 判断成功，这与文档不一致
- 本轮以接口文档和更通用的后端约定为准：优先按 `code === 0` 视为成功
- 为了兼容旧后端可能的漂移，实现层允许接受 `code === 0` 或 `code === 200`

## 3. 设计原则

### 3.1 先把入口打通，不做过度鉴权系统

本轮只建立“可进入系统”的最小闭环，不在登录阶段提前引入完整权限平台。

### 3.2 和旧系统保持关键行为兼容

以下兼容行为保留：

- 本地存储 `token`
- 本地存储 `jwt`
- 本地存储 `username`
- 本地存储 `isLoggedIn`
- 请求头使用 `Authorization: Bearer <token>`

这样可以降低后续共用接口和旧工具函数时的摩擦。

### 3.3 新前端不照搬旧实现细节

虽然行为兼容，但实现方式不再照搬 Vue 2：

- 不继续依赖 Vuex 写法
- 不继续依赖旧全局 `$http`
- 不把验证码逻辑和页面视觉强耦合
- 不把登录成功后的目标页写死成旧路由 `/main/visualization`

## 4. 结构设计

## 4.1 最小新增基础设施

为了让登录不是一次性脚本，`frontend` 需要补齐以下最小结构：

- `vue-router`
- `auth storage`
- `api client`
- `auth service`
- `route guard`

这是后续所有页面迁移都会依赖的底层。

### 4.2 推荐目录结构

建议新增如下模块：

- `src/router/`
  - 路由定义
  - 登录页与 Dashboard 页入口
  - `beforeEach` 守卫
- `src/lib/http/`
  - 统一 `fetch` 或轻量请求封装
  - 响应解析与错误归一化
- `src/modules/auth/`
  - `api.ts`
  - `storage.ts`
  - `types.ts`
  - `useAuth.ts` 或等价轻量状态封装

这一层只服务登录与进入系统，不引入完整状态管理库。

## 5. 登录页交互设计

### 5.1 交互流程

登录页的真实流程固定为：

1. 用户输入账号、密码、验证码
2. 前端执行本地表单校验
3. 前端对密码做 MD5
4. 前端调用 `/api/manager/login`
5. 后端成功返回 `jwt`
6. 前端写入登录态
7. 前端跳转到新 Dashboard 路由

### 5.2 表单校验

沿用旧页面的业务约束，但不要求完全复刻旧的提示文案：

- `username`: 必填
- `password`: 必填
- `code`: 必填

本轮不强制把旧系统里较强的正则限制全部照搬。原因是当前目标是打通真实接口，而不是复刻旧登录表单策略。

推荐做法：

- 必填校验保留
- 验证码长度保留 6 位约束
- 用户名和密码复杂度规则暂时放宽到非空即可

如果后端返回失败，再由后端 message 兜底反馈。

### 5.3 验证码策略

当前前端样板中的验证码是纯静态展示，不具备真实校验能力。由于本次并未确认后端存在独立验证码图片接口，本轮采用以下策略：

- 登录页仍保留前端输入框和验证码展示位
- 如果后端当前只校验 `code` 字段格式而不要求图片接口，则先使用本地刷新码方案
- 如果后端后续要求真实验证码接口，再在认证子任务中补接独立验证码服务

因此，本轮 spec 中 `code` 字段是登录必传字段，但验证码视觉与数据来源先允许本地生成。

## 6. 请求与响应设计

### 6.1 请求封装

新前端统一通过一个轻量 `api client` 发请求，要求：

- 默认 `base` 不写死，开发期依赖 `/api` 代理
- JSON 请求体
- 统一超时控制
- 统一错误对象

### 6.2 登录接口类型

建议定义前端类型：

```ts
type LoginRequest = {
  username: string
  password: string
  code: string
}

type LoginResponse = {
  code: number
  message: string
  data?: {
    username?: string
    jwt?: string
    token?: string
  }
}
```

兼容策略：

- 优先读取 `data.jwt`
- 如果后端未来返回 `data.token`，也允许兼容
- 最终统一映射成 `authToken`

### 6.3 成功判定

成功条件定义为：

- `response.code === 0 || response.code === 200`
- 且 `response.data.jwt || response.data.token` 存在

如果缺少 token/jwt，则视为失败。

### 6.4 错误反馈

登录失败统一分为三类：

- 业务失败
  - 后端返回合法响应，但 `code` 非成功值
- 协议失败
  - 返回成功码，但缺少 token/jwt
- 网络失败
  - 请求超时、代理失败、服务不可达

前端反馈策略：

- 优先展示后端 `message`
- 否则展示统一中文提示

## 7. 登录态设计

### 7.1 存储键

为兼容旧系统和后续迁移，本轮保留这些键：

- `token`
- `jwt`
- `username`
- `isLoggedIn`

写入规则：

- `token` 和 `jwt` 都写同一个最终 token 值
- `username` 来自后端返回值；如果后端不返回则回退到登录输入值
- `isLoggedIn` 固定写字符串 `"true"`

### 7.2 鉴权来源

新前端判断登录态时统一按以下顺序读取：

1. `localStorage.getItem('token')`
2. `localStorage.getItem('jwt')`

只要存在有效字符串，就认为已登录。

### 7.3 请求头注入

除登录接口外，后续所有受保护请求默认附加：

```http
Authorization: Bearer <token>
```

## 8. 路由与跳转设计

### 8.1 路由基线

本轮新增最小路由结构：

- `/login`
- `/dashboard`

并增加重定向规则：

- `/` -> 根据登录态跳转到 `/dashboard` 或 `/login`

### 8.2 登录后跳转

登录成功后统一进入：

- `/dashboard`

不再直接跳旧地址 `/main/visualization`。原因是新前端已经采用新的页面壳和新的信息架构，旧地址不应继续作为新工程入口。

### 8.3 路由守卫

守卫规则固定为：

- 未登录访问受保护页面，跳转 `/login`
- 已登录访问 `/login`，跳转 `/dashboard`

本轮只保护 Dashboard 及后续新增业务页。

## 9. Vite 代理设计

开发环境必须补充代理：

- `/api` -> `http://localhost:8080`

要求：

- 保留 `/api` 前缀再转发，还是重写前缀，要和后端实际路径一致
- 由于你已经明确真实接口是 `/api/manager/login`，推荐不重写前缀，直接代理整条路径

即浏览器请求：

- `/api/manager/login`

代理到：

- `http://localhost:8080/api/manager/login`

## 10. 对现有页面的影响

登录页从静态样板切到真实请求后，会带来以下连锁变化：

- `App.vue` 不能继续只靠本地 view switcher 切换登录和 dashboard
- 需要引入真实路由
- Dashboard 要成为登录成功后的正式目标页
- 登录页组件需要从纯展示组件升级为“展示 + 交互”组件

因此本轮不是只改一个按钮，而是把新前端从“视觉样板”推进到“真实可进入系统的壳”。

## 11. 验证标准

完成后应满足以下验证条件：

1. 本地开发环境启动后，访问 `/login` 可看到真实登录页
2. 输入账号、密码、验证码后，前端发出 `POST /api/manager/login`
3. 浏览器网络面板可看到请求走 `/api` 代理
4. 登录成功后，本地存储中出现 `token / jwt / username / isLoggedIn`
5. 登录成功后自动跳转到 `/dashboard`
6. 刷新 `/dashboard` 不会被重新踢回 `/login`
7. 手动清空 token 后再访问 `/dashboard` 会被重定向到 `/login`

## 12. 后续衔接

登录真实化完成后，后续任务的正确顺序是：

1. 接 Dashboard 的真实状态数据
2. 把顶部全局状态与左侧摘要块从静态 mock 改成接口数据
3. 再迁 `总览` 之外的第一页业务模块

也就是说，登录 spec 的完成标志不是“页面能提交”，而是“新前端已经具备真正承接业务的入口能力”。
