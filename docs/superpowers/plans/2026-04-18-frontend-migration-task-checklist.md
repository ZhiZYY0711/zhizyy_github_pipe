# Frontend Migration Task Checklist

**目标：** 在不偏离既有业务与设计系统的前提下，先完成 Vue 3 的真实登录页与总览仪表盘迁移，同时为其余模块产出可验收的 HTML/CSS 原型。

**当前约束：**

- 登录页和仪表盘进入 `frontend` Vue 3 工程
- 其余页面先落到 `docs/prototypes/`
- 所有页面必须严格服从 `docs/superpowers/specs/2026-04-17-frontend-replatform-design.md`
- 所有迁移边界必须服从 `docs/superpowers/specs/2026-04-18-frontend-pages-migration-design.md`
- 登录接口必须服从 `docs/superpowers/specs/2026-04-18-frontend-login-auth-design.md`

## 1. 文档与提示词基线

- [ ] 确认 `7.6 虚拟专家模块` 已恢复为具体可执行描述，而不是悬空引用
- [ ] 为以下模块分别提供独立提示词文件：
  - [ ] 登录页
  - [ ] 总览仪表盘
  - [ ] 监测模块
  - [ ] 资产模块
  - [ ] 任务模块
  - [ ] 应急模块
  - [ ] 日志模块
  - [ ] 虚拟专家模块

## 2. Vue 3 迁移任务

- [ ] 登录页迁移到 Vue 3
  - [ ] 接入 `/api/manager/login`
  - [ ] 配置 Vite `/api` 代理到 `http://localhost:8080`
  - [ ] 落地最小 `auth storage / auth service / route guard`
  - [ ] 保持已验收登录样板的视觉结构
- [ ] 仪表盘迁移到 Vue 3
  - [ ] 保持已验收单屏地图中心布局
  - [ ] 顶部导航包含醒目的 `虚拟专家` 顶层入口
  - [ ] 保持左侧图形摘要而不是长文本
  - [ ] 不引入底部趋势区和页面滚动

## 3. HTML/CSS 原型任务

- [ ] 监测模块原型
  - [ ] `数据监控页`
  - [ ] `监测数据页`
- [ ] 资产模块原型
- [ ] 任务模块原型
- [ ] 应急模块原型
- [ ] 日志模块原型
- [ ] 虚拟专家模块原型

## 4. 并行子 agent 写入边界

- [ ] 子 agent A：登录页与鉴权基础设施
  - 写入范围：`frontend/src/router/`、`frontend/src/modules/auth/`、`frontend/src/components/login/`、`frontend/vite.config.ts`、必要的 `App.vue/main.ts`
- [ ] 子 agent B：总览仪表盘 Vue 3 页面
  - 写入范围：`frontend/src/modules/dashboard/`、`frontend/src/components/shell/`、必要的 `App.vue`
- [ ] 子 agent C：监测模块 HTML/CSS 原型
  - 写入范围：`docs/prototypes/monitoring-*.html`、`docs/prototypes/monitoring-*.css`
- [ ] 子 agent D：资产与任务模块 HTML/CSS 原型
  - 写入范围：`docs/prototypes/assets-*.html`、`docs/prototypes/assets-*.css`、`docs/prototypes/tasks-*.html`、`docs/prototypes/tasks-*.css`
- [ ] 子 agent E：应急、日志、虚拟专家 HTML/CSS 原型
  - 写入范围：`docs/prototypes/emergency-*.html`、`docs/prototypes/emergency-*.css`、`docs/prototypes/logs-*.html`、`docs/prototypes/logs-*.css`、`docs/prototypes/virtual-expert-*.html`、`docs/prototypes/virtual-expert-*.css`

## 5. 审核与验证

- [ ] 逐个阅读子 agent 改动的实际代码，不只看其口头总结
- [ ] 对登录页进行 API 契约复核
- [ ] 对 Vue 3 工程执行构建验证
- [ ] 对每个 HTML/CSS 原型检查结构、布局和设计系统一致性
- [ ] 对不符合 spec 的改动进行二次修正
