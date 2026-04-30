# Agent Module Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the virtual expert Agent session API into clear service boundaries while preserving API behavior and improving run/SSE stability diagnostics.

**Architecture:** Keep FastAPI routes in `app/api/sessions.py` as the compatibility entrypoint. Move runtime creation, export intent planning, session/message flow, and stream/run flow into `app/services/*` modules. Preserve existing tests and module-level monkeypatch seams by keeping thin aliases in `sessions.py` during this first refactor.

**Tech Stack:** FastAPI, Pydantic v2, pytest, async Python, SQLAlchemy repository, Redis state, SSE via `StreamingResponse`.

---

### Task 1: Add Stability Regression Tests

**Files:**
- Modify: `server-agent/tests/test_sessions_api.py`

- [ ] **Step 1: Add tests for stream replay and disconnect semantics**

Add focused tests near the existing stream tests:

```python
def test_completed_in_memory_run_stream_replays_events(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "回放会话"}).json()
    message = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "压力异常"},
    ).json()
    stream_url = f"/api/agent/sessions/{created['session_id']}/runs/{message['run']['id']}/stream"
    with client.stream("GET", stream_url) as response:
        response.read()

    with client.stream("GET", stream_url) as replay:
        body = replay.read().decode("utf-8")

    assert replay.status_code == 200
    assert '"type":"run_started"' in body
    assert '"status":"completed"' in body
```

- [ ] **Step 2: Run the new test and confirm it exposes current behavior**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py::test_completed_in_memory_run_stream_replays_events -q
```

Expected before implementation: failure with 409 because completed runs currently reject repeated stream calls.

- [ ] **Step 3: Add status metadata assertions to existing run tests**

Extend existing assertions so completed runs expose `event_count`, `completed_at`/`finished_at`, and no active run remains in memory mode:

```python
session = sessions_module._get_session(created["session_id"])
run = session["runs"][message_body["run"]["id"]]
assert run["status"] == "completed"
assert run["event_count"] == 4
assert run["last_event_seq"] == 4
assert run["failure_reason"] is None
assert session["active_run_id"] is None
```

- [ ] **Step 4: Run the focused test file**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py -q
```

Expected before implementation: only new metadata/replay expectations fail; unrelated tests stay green.

### Task 2: Extract Runtime and Export Services

**Files:**
- Create: `server-agent/app/services/runtime_factory.py`
- Create: `server-agent/app/services/export_intent_service.py`
- Modify: `server-agent/app/api/sessions.py`
- Test: `server-agent/tests/test_sessions_api.py`

- [ ] **Step 1: Create `runtime_factory.py`**

Move runtime creation into:

```python
from app.agent.service import AgentRuntimeService
from app.tools.business_tools import build_business_registry


def build_runtime_service(summary_memory: list[str] | None = None) -> AgentRuntimeService:
    try:
        return AgentRuntimeService(
            tool_registry=build_business_registry(),
            summary_memory=summary_memory or [],
        )
    except TypeError:
        return AgentRuntimeService(tool_registry=build_business_registry())
```

- [ ] **Step 2: Create `export_intent_service.py`**

Move `ExportPlanSection`, `ExportPlanTable`, `ExportIntentPlan`, `EXPORT_INTENT_SYSTEM_PROMPT`, `infer_export_plan`, and `export_plan_from_intent` into this service. Keep `build_llm_client` imported in the service, and keep a `sessions.py` wrapper so existing tests can still monkeypatch `sessions_module.build_llm_client`.

- [ ] **Step 3: Update `sessions.py` wrappers**

Replace direct helper implementation with thin wrappers:

```python
def _build_runtime_service(summary_memory: list[str] | None = None) -> AgentRuntimeService:
    return runtime_factory.build_runtime_service(summary_memory)


async def _infer_export_plan(content: str) -> dict[str, Any] | None:
    return await export_intent_service.infer_export_plan(content, llm_client_factory=build_llm_client)
```

- [ ] **Step 4: Verify export and runtime tests**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py::test_post_message_uses_llm_export_plan_without_run tests/test_sessions_api.py::test_run_session_returns_events -q
```

Expected: both tests pass.

### Task 3: Extract Session and Message Services

**Files:**
- Create: `server-agent/app/services/session_service.py`
- Create: `server-agent/app/services/message_service.py`
- Modify: `server-agent/app/api/sessions.py`
- Test: `server-agent/tests/test_sessions_api.py`

- [ ] **Step 1: Create `session_service.py`**

Move create/list/delete session behavior into functions that accept dependency callables:

```python
async def create_session(request, *, use_in_memory_store, get_repository, sessions, now_iso):
    ...
```

Keep behavior unchanged for both memory and persistent modes.

- [ ] **Step 2: Create `message_service.py`**

Move `create_message` behavior into a function that receives `infer_export_plan`, `active_run_response`, `message_response`, and `run_created_response`. Preserve current 409 response shape for active runs.

- [ ] **Step 3: Update route handlers**

Change `create_session`, `list_sessions`, `delete_session`, and `create_message` route functions in `sessions.py` to call the service modules and return their result unchanged.

- [ ] **Step 4: Verify session/message contracts**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py::test_create_session_returns_session_id tests/test_sessions_api.py::test_list_sessions_returns_recent_session tests/test_sessions_api.py::test_delete_session_removes_it_from_history tests/test_sessions_api.py::test_post_message_rejects_second_message_while_run_active -q
```

Expected: all selected tests pass.

### Task 4: Extract Stream and Run Services

**Files:**
- Create: `server-agent/app/services/run_service.py`
- Create: `server-agent/app/services/stream_service.py`
- Modify: `server-agent/app/api/sessions.py`
- Test: `server-agent/tests/test_sessions_api.py`

- [ ] **Step 1: Create `run_service.py`**

Move `_derive_run_status`, `_complete_in_memory_message_run`, `_build_run_summary`, memory-candidate helpers, and non-stream run execution into this module. Expose wrappers from `sessions.py` for compatibility.

- [ ] **Step 2: Create `stream_service.py`**

Move `_sse_event`, `_log_agent_event`, in-memory specific stream, persistent specific stream, and persistent session stream into this module. Add replay behavior for terminal runs and avoid marking a client disconnect as `failed`.

- [ ] **Step 3: Add run metadata on completion**

For in-memory runs, set:

```python
run["event_count"] = len(events)
run["last_event_seq"] = events[-1]["seq"] if events else 0
run["failure_reason"] = None if final_status != "failed" else "runtime ended without terminal success event"
run["completed_at"] = now
```

For persistent runs, use existing model columns where available: `started_at`, `finished_at`, `error_message`. Do not require a schema migration for this first pass unless tests prove the current schema lacks those columns.

- [ ] **Step 4: Verify stream and run behavior**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py::test_stream_run_session_returns_sse_events tests/test_sessions_api.py::test_post_message_creates_run_and_run_specific_stream_persists_summary tests/test_sessions_api.py::test_completed_in_memory_run_stream_replays_events -q
```

Expected: all selected tests pass.

### Task 5: Full Verification

**Files:**
- No new files expected.

- [ ] **Step 1: Run focused Agent tests**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest tests/test_sessions_api.py tests/test_react_runtime.py tests/test_persistence_models.py -q
```

Expected: all tests pass.

- [ ] **Step 2: Run all Agent tests**

Run:

```bash
cd server-agent
PYTHONPATH=. .venv/bin/pytest -q
```

Expected: all tests pass.

- [ ] **Step 3: Run Java proxy compile check**

Run:

```bash
cd server
mvn -pl server-web -am test-compile
```

Expected: build success.

- [ ] **Step 4: Inspect diff for scope**

Run:

```bash
git diff --stat
git diff --check
```

Expected: only Agent service split, tests, and this plan changed for this task; no whitespace errors.
