# 虚拟专家页面自动化测试报告

测试时间：2026-05-03 23:12-23:22 CST  
测试入口：`http://localhost:5173/virtual-expert`  
测试账号：`admin001`  
工具：`playwright-cli`、浏览器快照、接口请求记录、PostgreSQL 结构校验

## 环境

- Frontend `:5173`、server-web `:8080`、server-agent `:8000` 均已运行。
- MySQL、PostgreSQL、Redis、Qdrant 端口均可访问。
- 通过真实登录进入页面，未跳过前端鉴权。

## 覆盖范围

| 功能 | 结果 | 证据 |
|---|---|---|
| 登录跳转与页面加载 | 通过 | `/login` 登录后进入 `/virtual-expert`，页面显示虚拟专家工作台、会话列表、输入框 |
| 侧栏折叠/展开 | 通过 | `收起列表` 后出现 `展开列表`，再次点击恢复 |
| 历史搜索 | 通过 | 搜索 `北段` 后仅显示匹配会话，`Esc` 收起搜索框 |
| 偏好面板 | 通过 | `偏好` 打开 `我的偏好`，`关闭` 后面板消失 |
| 模型挡位选择 | 通过 | `Auto` 可切换到 `标准` 并回到 `Auto` |
| 图片上传/移除 | 通过 | 上传 `frontend/src/assets/hero.png` 后出现附件，点击移除后消失 |
| 新对话与输入换行 | 通过 | `新对话` 清空当前上下文，`Shift+Enter` 在输入框内换行 |
| 会话重命名 | 通过 | 测试会话可通过 prompt 重命名 |
| 置顶/取消置顶 | 通过 | 菜单操作后显示 `置顶`，取消置顶后恢复 |
| 归档/恢复 | 通过 | 会话可进入归档列表并恢复到普通列表 |
| 分享链接 | 初测失败，修复后通过 | 初测 `POST /share` 500；应用迁移后浏览器显示 `分享链接已生成`，接口 200 |
| 分享 Markdown | 初测失败，修复后通过 | 应用迁移后浏览器显示 `Markdown 分享已生成`，接口 200 且返回 markdown |
| 导出 PDF | 通过 | `POST /virtual-expert/agent/exports` 返回 200，页面生成下载提示 |
| 发送消息与 SSE | 通过 | `POST /messages` 返回 200，`/runs/{runId}/stream` 返回 200，页面显示 Agent Run |
| 停止运行 | 通过 | 较长请求中停止按钮变为可用，点击后 `POST /cancel` 返回 200，页面显示 `已取消` |
| 移动端基础布局 | 通过 | 390x844 下会话操作区和输入框可见 |
| 控制台错误 | 通过 | 修复后 `playwright-cli console error/warning` 为 0 |

## 缺陷与修复

### VE-001 分享功能 500

现象：

- 点击 `分享链接` 时页面显示 `Internal Server Error`。
- 请求记录：`POST /api/manager/virtual-expert/agent/sessions/{id}/share => 500`。

根因：

- server-agent 代码和迁移已定义 `agent_share`。
- 当前 PostgreSQL `pipeline_agent` 未应用 `database/postgres/02-migrations/2026_05_03_agent_model_share.sql`，实际缺少 `agent_share` 表。

修复：

```bash
docker exec -i pipeline-dev-postgres psql -U agent -d pipeline_agent < database/postgres/02-migrations/2026_05_03_agent_model_share.sql
```

修复由 sub agent 执行；未改动代码文件。

审查验证：

- `\d agent_share` 显示主键、外键、`share_type` check、`idx_agent_share_session` 均存在。
- `POST /share {"type":"link"}` 返回 200，包含 `shareUrl`。
- `POST /share {"type":"md"}` 返回 200，包含 markdown。
- 浏览器回归：`分享链接已生成`、`Markdown 分享已生成` 均出现。

## 备注

- 当前仓库在测试前已有多处未提交改动，本次只新增本报告和临时 Playwright 脚本；未回退或覆盖已有改动。
- 本地数据库状态已被修复。其他环境拉取代码后仍需手动应用同一个 PostgreSQL migration。
