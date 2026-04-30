# Agent 模块一期优化设计

日期：2026-04-30

## 背景

`server-agent` 已经承担虚拟专家会话、消息、运行、SSE、导出意图、ReAct 推理、工具代理、RAG 和持久化等职责。当前主要风险不是某一个算法点，而是入口文件职责过厚：`app/api/sessions.py` 同时管理 HTTP 路由、运行状态、内存/持久化分支、SSE 输出、导出意图和 runtime 创建，后续排查 SSE、取消、导出或推理质量问题时容易互相牵连。

本期优化采用“方案 A 为主，附带方案 C”：先拆清结构和运行状态边界，同时补最小可观测能力。智能效果优化放到下一期。

## 目标

1. 保持现有 API 合同不变，前端仍通过 `/manager/virtual-expert/agent/...` 访问 Spring 代理，再由 Spring 调用 Agent。
2. 将 `sessions.py` 从大入口拆成薄 API 层和若干服务层，降低后续修改风险。
3. 收紧 run 状态机和 SSE 事件流行为，避免取消、断连、异常、重复 stream 时状态不一致。
4. 增加最小运行诊断字段和日志，便于定位 `server-web -> server-agent -> runtime` 链路问题。
5. 以现有测试为保护网，补足运行状态和 SSE 稳定性测试。

## 非目标

1. 不重写 ReAct 推理策略、提示词体系或工具选择算法。
2. 不更换 LLM、Qdrant、Redis、PostgreSQL 等基础设施。
3. 不改 Spring 代理路径和前端业务合同。
4. 不把内存模式删除；内存模式仍用于本地调试。
5. 不做完整链路追踪平台，只补运行排障需要的最小观测信息。

## 模块边界

目标结构如下：

```text
server-agent/app/
├── api/
│   └── sessions.py              # FastAPI 路由、请求响应模型、HTTP 状态码
├── services/
│   ├── session_service.py        # 创建、列表、删除会话
│   ├── message_service.py        # 追加消息、导出意图判断、创建 run
│   ├── run_service.py            # 启动、完成、失败、取消 run
│   ├── stream_service.py         # SSE replay、执行、断连处理、事件格式化
│   ├── export_intent_service.py  # 导出意图规划
│   └── runtime_factory.py        # 创建 runtime、tool registry、retriever、LLM client
├── persistence/                  # 保持现有 repository 和模型职责
├── react/                        # 保持 ReAct runtime 职责
├── tools/                        # 保持工具代理和归一化职责
└── rag/                          # 保持检索职责
```

边界原则：

- API 层只处理 HTTP，不直接分散管理内存/持久化细节。
- service 层管理业务流程和状态转换。
- runtime 只负责推理和产出事件。
- repository 只负责存取，不承担运行策略。
- 导出意图规划独立，避免导出逻辑继续混在消息创建流程里。

## 运行状态机

run 状态固定为：

```text
created -> running -> completed
                   -> failed
                   -> cancelled
```

约束：

- `POST /sessions/{session_id}/messages` 追加用户消息并创建 `created` run。
- `GET /sessions/{session_id}/runs/{run_id}/stream` 或当前 run stream 负责启动执行。
- 已终态 run 再次 stream 只 replay 已持久化事件，不重复执行 runtime。
- 同一个 session 同一时间只允许一个 active run。
- 所有终态都释放 `active_run_id`。
- `run_completed`、`run_failed`、`run_cancelled` 三类终态事件同一个 run 只能出现一个。

## SSE 行为

SSE 执行流程：

1. 校验 session 和 run 是否存在。
2. 根据 `afterSeq` replay 已有事件。
3. 如果 run 已是终态，replay 后结束。
4. 如果 run 是 `created`，抢占启动权并置为 `running`。
5. 执行 runtime，逐条持久化事件，再发送给客户端。
6. 每轮发送前检查 cancel flag。
7. runtime 正常结束写 `run_completed`。
8. runtime 异常写 `run_failed`，记录失败原因。
9. 取消写 `run_cancelled`，并释放 active run。

客户端断开不等于 run 失败。断开时记录 `client_disconnected` 诊断日志；已产生的事件仍可通过 events 或后续 stream replay 读取。是否继续后台执行保持当前实现可控，不把浏览器刷新、切页或代理超时误判为业务失败。

## 最小观测

每个 run 建议记录：

- `started_at`
- `finished_at`
- `failure_reason`
- `event_count`
- `last_event_seq`

日志字段统一包含：

- `session_id`
- `run_id`
- `seq`
- `event_type`
- `run_status`

需要明确区分的诊断场景：

- `run_cancel_requested`
- `run_cancelled`
- `client_disconnected`
- `runtime_failed`
- `stream_replayed`
- `stream_started`
- `stream_completed`

这些信息先进入现有日志和事件 payload，不引入新的监控系统。

## 测试策略

保留并扩展现有测试，优先覆盖合同和状态一致性。

保合同测试：

- 创建会话
- 列表会话
- 删除会话
- 追加消息
- 当前 run stream
- 指定 run stream
- events 查询
- cancel
- export plan

新增或强化状态测试：

- 创建消息后 run 为 `created`。
- stream 启动后 run 为 `running`。
- runtime 正常结束后 run 为 `completed`，并释放 active run。
- runtime 异常后 run 为 `failed`，并保留失败原因。
- cancel 后 run 为 `cancelled`，再次 stream 只 replay。
- 已终态 run 再次 stream 不重复执行 runtime。

新增或强化 SSE 测试：

- SSE 事件 `seq` 递增。
- `afterSeq` 能续读。
- 客户端断开不写 `failed`。
- 内存模式和持久化模式覆盖关键路径。
- 每个 run 只有一个终态事件。

推荐验证命令：

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py tests/test_react_runtime.py tests/test_persistence_models.py -q
PYTHONPATH=. .venv/bin/pytest -q
```

如果 Java 代理合同相关文件被影响，再运行：

```bash
cd server
mvn -pl server-web -am test-compile
```

## 实施约束

1. 先补或调整测试，再拆实现。
2. 每次拆分只迁移一类职责，避免 API 行为和 runtime 行为同时变化。
3. 保留现有响应字段和事件类型，新增字段必须向后兼容。
4. 不改 `.env` 配置语义，不强推新的部署方式。
5. 不修改前端，除非后续验证发现前端依赖了不稳定行为。

## 验收标准

1. `server-agent` 全量 pytest 通过。
2. `server-web` 相关测试编译通过。
3. 现有 `/manager/virtual-expert/agent/...` 代理路径不变。
4. `sessions.py` 不再承载导出意图、runtime 创建、SSE 执行和状态转换的全部细节。
5. run 的 completed、failed、cancelled 终态可从事件和状态中一致判断。
6. SSE replay、取消、断连、异常路径都有测试或明确日志证据。
