import os
from typing import Any

import pytest
from fastapi.testclient import TestClient

os.environ["AGENT_USE_IN_MEMORY_STORE"] = "true"
os.environ["AGENT_REQUIRE_INTERNAL_AUTH"] = "false"

from app.main import app
from app.agent.events import AgentEvent, AgentEventType
from app.api import sessions as sessions_module


class FastCompletedService:
    def __init__(self, tool_registry) -> None:
        self.tool_registry = tool_registry

    async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
        yield AgentEvent(
            id=f"evt_{run_id}_001",
            session_id=session_id,
            run_id=run_id,
            seq=1,
            type=AgentEventType.RUN_STARTED,
        )
        yield AgentEvent(
            id=f"evt_{run_id}_002",
            session_id=session_id,
            run_id=run_id,
            seq=2,
            type=AgentEventType.PLAN_CREATED,
        )
        yield AgentEvent(
            id=f"evt_{run_id}_003",
            session_id=session_id,
            run_id=run_id,
            seq=3,
            type=AgentEventType.RUN_COMPLETED,
        )


def test_create_session_returns_session_id():
    client = TestClient(app)
    response = client.post(
        "/api/agent/sessions",
        json={"raw_input": "CP-04 阴保电位波动超过阈值", "source_type": "manual"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["session_id"].startswith("ana_")
    assert body["status"] == "created"


def test_list_sessions_returns_recent_session():
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "左侧历史会话"}).json()

    response = client.get("/api/agent/sessions", params={"limit": 5})

    assert response.status_code == 200
    sessions = response.json()["sessions"]
    assert any(session["id"] == created["session_id"] for session in sessions)


def test_delete_session_removes_it_from_history():
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "待删除会话"}).json()

    response = client.delete(f"/api/agent/sessions/{created['session_id']}")
    sessions = client.get("/api/agent/sessions", params={"limit": 20}).json()["sessions"]

    assert response.status_code == 200
    assert response.json() == {"session_id": created["session_id"], "status": "deleted"}
    assert all(session["id"] != created["session_id"] for session in sessions)


def test_run_session_returns_events(monkeypatch):
    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id="evt_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.PLAN_CREATED,
            )
            yield AgentEvent(
                id="evt_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.AWAITING_USER,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "压力波动"}).json()
    response = client.post(f"/api/agent/sessions/{created['session_id']}/runs")

    assert response.status_code == 200
    event_types = [event["type"] for event in response.json()["events"]]
    assert "plan_created" in event_types
    assert "awaiting_user" in event_types


def test_stream_run_session_returns_sse_events(monkeypatch):
    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id="evt_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.RUN_STARTED,
            )
            yield AgentEvent(
                id="evt_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.RUN_COMPLETED,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "压力波动"}).json()

    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/stream") as response:
        body = response.read().decode("utf-8")

    assert response.status_code == 200
    assert "text/event-stream" in response.headers["content-type"]
    assert "event: agent_event" in body
    assert '"type":"run_started"' in body
    assert '"type":"run_completed"' in body


def test_stream_run_session_emits_preparation_progress_before_runtime_events(monkeypatch):
    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id="evt_runtime_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.RUN_STARTED,
            )
            yield AgentEvent(
                id="evt_runtime_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.RUN_COMPLETED,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "压力波动"}).json()

    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/stream") as response:
        body = response.read().decode("utf-8")

    assert '"type":"run_preparation_completed"' in body
    assert body.index('"type":"run_preparation_completed"') < body.index('"type":"run_started"')
    assert '"step":"stream_connected"' in body
    assert '"step":"memory_recall"' in body
    assert '"step":"runtime_ready"' in body


def test_post_message_creates_run_and_run_specific_stream_persists_summary(monkeypatch):
    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id=f"evt_{run_id}_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.RUN_STARTED,
                payload={"raw_input": raw_input},
            )
            yield AgentEvent(
                id=f"evt_{run_id}_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.FINAL_ANSWER_DELTA,
                payload={"delta": "建议先复核上游阀室。"},
            )
            yield AgentEvent(
                id=f"evt_{run_id}_003",
                session_id=session_id,
                run_id=run_id,
                seq=3,
                type=AgentEventType.RECOMMENDATION_GENERATED,
                payload={
                    "summary": "上游阀室优先复核",
                    "risk_level": "high",
                    "recommended_actions": ["核对阀门开度"],
                    "missing_information": ["现场开度"],
                },
            )
            yield AgentEvent(
                id=f"evt_{run_id}_004",
                session_id=session_id,
                run_id=run_id,
                seq=4,
                type=AgentEventType.RUN_COMPLETED,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "CP-04 连续研判"}).json()
    message_response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "CP-04 压力异常怎么处理？"},
    )

    assert message_response.status_code == 200
    message_body = message_response.json()
    assert message_body["message"]["role"] == "user"
    assert message_body["run"]["status"] == "created"

    with client.stream(
        "GET",
        f"/api/agent/sessions/{created['session_id']}/runs/{message_body['run']['id']}/stream",
    ) as response:
        body = response.read().decode("utf-8")

    assert response.status_code == 200
    assert '"type":"final_answer_delta"' in body
    assert '"status":"completed"' in body

    timeline = client.get(
        f"/api/agent/sessions/{created['session_id']}/timeline",
        params={"limit": 1},
    )

    assert timeline.status_code == 200
    items = timeline.json()["items"]
    assert len(items) == 1
    assert items[0]["userMessage"]["content"] == "CP-04 压力异常怎么处理？"
    assert items[0]["run"]["id"] == message_body["run"]["id"]
    assert items[0]["run"]["summary"]["finalAnswer"] == "建议先复核上游阀室。"
    assert items[0]["run"]["summary"]["riskLevel"] == "high"

    session = sessions_module._get_session(created["session_id"])
    run = session["runs"][message_body["run"]["id"]]
    assert run["status"] == "completed"
    assert run["event_count"] == 4
    assert run["last_event_seq"] == 4
    assert run["failure_reason"] is None
    assert session["active_run_id"] is None


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


def test_react_export_tool_creates_export_event_after_run_summary(monkeypatch):
    class ExportService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id=f"evt_{run_id}_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.TOOL_COMPLETED,
                payload={
                    "tool_name": "create_export_report",
                    "context": {
                        "export_plan": {
                            "format": "pdf",
                            "scope": "latest_turn",
                            "title": "压力异常研判报告",
                        }
                    },
                    "raw": {"export_plan": {"format": "pdf", "scope": "latest_turn", "title": "压力异常研判报告"}},
                    "summary": "已准备 PDF 导出计划。",
                },
            )
            yield AgentEvent(
                id=f"evt_{run_id}_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.RECOMMENDATION_GENERATED,
                payload={
                    "summary": "压力异常需要复核",
                    "risk_level": "medium",
                    "recommended_actions": ["复核压力趋势"],
                },
            )
            yield AgentEvent(
                id=f"evt_{run_id}_003",
                session_id=session_id,
                run_id=run_id,
                seq=3,
                type=AgentEventType.RUN_COMPLETED,
            )

    async def fake_create_export_file(session_id: str, run_id: str, export_plan: dict[str, Any]):
        return {
            "exportId": "exp_test",
            "fileName": "pressure-report.pdf",
            "downloadUrl": "/manager/virtual-expert/agent/exports/exp_test/download",
        }

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", ExportService)
    monkeypatch.setattr(sessions_module, "_create_export_file", fake_create_export_file)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()
    message = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "分析压力异常并导出 PDF"},
    ).json()

    with client.stream(
        "GET",
        f"/api/agent/sessions/{created['session_id']}/runs/{message['run']['id']}/stream",
    ) as response:
        body = response.read().decode("utf-8")

    assert '"type":"export_created"' in body
    assert "pressure-report.pdf" in body
    assert body.index('"type":"run_completed"') < body.index('"type":"export_created"')


def test_post_message_rejects_second_message_while_run_active():
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "运行中会话"}).json()
    first = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "先分析压力异常"},
    )

    second = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "继续追问"},
    )

    assert first.status_code == 200
    assert second.status_code == 409
    assert second.json()["code"] == "SESSION_RUN_IN_PROGRESS"


def test_post_message_always_creates_run_for_plain_analysis():
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "普通研判"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "先分析压力异常"},
    )

    assert response.status_code == 200
    body = response.json()
    assert "export_plan" not in body
    assert body["run"]["status"] == "created"


def test_post_message_export_words_still_enter_react_run():
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "请先分析压力异常，并把这次研判导出 PDF"},
    )

    assert response.status_code == 200
    body = response.json()
    assert "export_plan" not in body
    assert body["run"]["status"] == "created"
    assert body["run"]["streamUrl"].endswith(f"/runs/{body['run']['id']}/stream")


def test_timeline_pages_backwards_from_latest_turn(monkeypatch):
    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id=f"evt_{run_id}_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.FINAL_ANSWER_DELTA,
                payload={"delta": f"回答：{raw_input}"},
            )
            yield AgentEvent(
                id=f"evt_{run_id}_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.RUN_COMPLETED,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "分页会话"}).json()
    run_ids = []
    for content in ["第一轮", "第二轮", "第三轮"]:
        message = client.post(
            f"/api/agent/sessions/{created['session_id']}/messages",
            json={"content": content},
        ).json()
        run_ids.append(message["run"]["id"])
        with client.stream(
            "GET",
            f"/api/agent/sessions/{created['session_id']}/runs/{message['run']['id']}/stream",
        ) as response:
            response.read()

    latest = client.get(f"/api/agent/sessions/{created['session_id']}/timeline", params={"limit": 1})
    latest_body = latest.json()

    older = client.get(
        f"/api/agent/sessions/{created['session_id']}/timeline",
        params={"limit": 2, "beforeCursor": latest_body["beforeCursor"]},
    )

    assert [item["userMessage"]["content"] for item in latest_body["items"]] == ["第三轮"]
    assert latest_body["hasMoreBefore"] is True
    assert [item["userMessage"]["content"] for item in older.json()["items"]] == ["第一轮", "第二轮"]
    assert older.json()["hasMoreBefore"] is False


def test_legacy_accepted_summary_memory_is_still_injected_into_new_run(monkeypatch):
    class MemoryAwareService:
        def __init__(self, tool_registry, summary_memory=None) -> None:
            self.summary_memory = summary_memory or []

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id=f"evt_{run_id}_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.CONTEXT_BUILT,
                payload={
                    "summary_memory_count": len(self.summary_memory),
                    "summary_memory": self.summary_memory,
                },
            )
            yield AgentEvent(
                id=f"evt_{run_id}_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.RECOMMENDATION_GENERATED,
                payload={"summary": "已参考历史记忆", "recommended_actions": ["复核重点管段"]},
            )
            yield AgentEvent(
                id=f"evt_{run_id}_003",
                session_id=session_id,
                run_id=run_id,
                seq=3,
                type=AgentEventType.RUN_COMPLETED,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", MemoryAwareService)
    sessions_module._memory_candidates.clear()
    sessions_module._memory_candidates["mem_legacy"] = {
        "id": "mem_legacy",
        "session_id": "ana_legacy",
        "run_id": "run_legacy",
        "memory_type": "preference",
        "content": "CP-04 是重点管段",
        "status": "accepted",
    }

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "新会话"}).json()
    message = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "CP-04 压力波动怎么处理"},
    ).json()
    with client.stream(
        "GET",
        f"/api/agent/sessions/{created['session_id']}/runs/{message['run']['id']}/stream",
    ) as response:
        body = response.read().decode("utf-8")

    assert '"summary_memory_count":1' in body
    assert "CP-04 是重点管段" in body


def test_low_risk_preference_emits_memory_accepted_and_is_recalled(monkeypatch):
    class MemoryService:
        def __init__(self, tool_registry, summary_memory=None, preference_memory=None) -> None:
            self.preference_memory = preference_memory or summary_memory or []

        async def run_analysis(
            self,
            session_id: str,
            run_id: str,
            raw_input: str,
            permissions: set[str],
            conversation_context=None,
        ):
            yield AgentEvent(
                id=f"evt_{run_id}_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.CONTEXT_BUILT,
                payload={"preference_memory": self.preference_memory},
            )
            yield AgentEvent(
                id=f"evt_{run_id}_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.RUN_COMPLETED,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", MemoryService)
    sessions_module._memory_candidates.clear()
    getattr(sessions_module, "_user_memories", {}).clear()
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "偏好测试"}).json()
    first = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "以后回答先给结论"},
    ).json()

    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/{first['run']['id']}/stream") as response:
        body = response.read().decode("utf-8")

    assert '"type":"memory_accepted"' in body
    assert "回答先给结论" in body

    second = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "压力波动怎么处理"},
    ).json()
    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/{second['run']['id']}/stream") as response:
        recalled = response.read().decode("utf-8")

    assert '"type":"memory_recalled"' in recalled
    assert "回答先给结论" in recalled


def test_high_risk_preference_creates_pending_candidate_and_accepts_it(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    sessions_module._memory_candidates.clear()
    getattr(sessions_module, "_user_memories", {}).clear()
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "高影响偏好"}).json()
    message = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "以后风险都按高等级处理"},
    ).json()

    with client.stream("GET", f"/api/agent/sessions/{created['session_id']}/runs/{message['run']['id']}/stream") as response:
        body = response.read().decode("utf-8")

    assert '"type":"memory_candidate_created"' in body
    pending = client.get("/api/agent/memories", params={"status": "pending"}).json()["items"]
    assert len(pending) == 1
    assert pending[0]["riskLevel"] == "high"

    accepted = client.post(f"/api/agent/memory-candidates/{pending[0]['id']}/accept")

    assert accepted.status_code == 200
    assert accepted.json()["memory"]["status"] == "active"
    active = client.get("/api/agent/memories", params={"status": "active"}).json()["items"]
    assert any(item["content"] == pending[0]["content"] for item in active)


def test_memory_list_is_scoped_by_user_header(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    sessions_module._memory_candidates.clear()
    getattr(sessions_module, "_user_memories", {}).clear()
    client = TestClient(app)
    first = client.post("/api/agent/sessions", json={"title": "u1"}, headers={"X-Agent-User-Id": "u1"}).json()
    message = client.post(
        f"/api/agent/sessions/{first['session_id']}/messages",
        json={"content": "以后回答短一点"},
        headers={"X-Agent-User-Id": "u1"},
    ).json()
    with client.stream(
        "GET",
        f"/api/agent/sessions/{first['session_id']}/runs/{message['run']['id']}/stream",
        headers={"X-Agent-User-Id": "u1"},
    ) as response:
        response.read()

    assert all(item.get("user_id") for item in sessions_module._memory_candidates.values())
    assert client.get("/api/agent/memories", headers={"X-Agent-User-Id": "u1"}).json()["items"]
    assert client.get("/api/agent/memories", headers={"X-Agent-User-Id": "u2"}).json()["items"] == []


def test_run_session_exposes_all_default_read_tools(monkeypatch):
    captured_permissions = set()

    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            captured_permissions.update(permissions)
            yield AgentEvent(
                id="evt_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.RUN_COMPLETED,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "压力波动"}).json()
    response = client.post(f"/api/agent/sessions/{created['session_id']}/runs")

    assert response.status_code == 200
    assert sessions_module.DEFAULT_RUN_PERMISSIONS.issubset(captured_permissions)
    assert {"equipment:read", "case:read"}.issubset(captured_permissions)


def test_list_events_filters_after_seq(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "压力波动"}).json()
    run = client.post(f"/api/agent/sessions/{created['session_id']}/runs").json()

    response = client.get(f"/api/agent/sessions/{created['session_id']}/events", params={"after_seq": 2})

    assert response.status_code == 200
    assert response.json()["session_id"] == created["session_id"]
    assert response.json()["run_id"] == run["run_id"]
    assert response.json()["events"]
    assert all(event["seq"] > 2 for event in response.json()["events"])


def test_run_session_rejects_second_run_to_preserve_event_cursor(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "压力波动"}).json()
    client.post(f"/api/agent/sessions/{created['session_id']}/runs")

    response = client.post(f"/api/agent/sessions/{created['session_id']}/runs")

    assert response.status_code == 409


def test_create_session_rejects_blank_raw_input():
    client = TestClient(app)
    response = client.post("/api/agent/sessions", json={"raw_input": "   "})

    assert response.status_code == 422


def test_unknown_session_returns_404():
    client = TestClient(app)
    response = client.get("/api/agent/sessions/ana_missing/events")

    assert response.status_code == 404


def test_cancel_run_returns_cancel_requested(monkeypatch):
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "pressure anomaly"}).json()
    run = client.post(f"/api/agent/sessions/{created['session_id']}/runs").json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/runs/{run['run_id']}/cancel"
    )

    assert response.status_code == 200
    assert response.json() == {
        "session_id": created["session_id"],
        "run_id": run["run_id"],
        "status": "cancel_requested",
    }


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("terminal_event_type", "expected_status"),
    [
        (AgentEventType.RUN_CANCELLED, "cancelled"),
        (AgentEventType.AWAITING_USER, "awaiting_user"),
    ],
)
async def test_persistent_run_session_preserves_terminal_run_status(
    monkeypatch,
    terminal_event_type,
    expected_status,
):
    class FakeRedisState:
        async def remember_last_seq(self, session_id: str, seq: int) -> None:
            return None

    class FakeRepository:
        def __init__(self) -> None:
            self.session = {
                "session_id": "ana_001",
                "status": "running",
                "raw_input": "压力波动",
                "run_id": None,
            }
            self.completed_status: str | None = None

        async def get_session(self, session_id: str):
            return self.session if session_id == self.session["session_id"] else None

        async def create_run(self, session_id: str, run_id: str) -> None:
            self.session["run_id"] = run_id

        async def append_event(self, event) -> None:
            return None

        async def complete_run(self, session_id: str, run_id: str, status: str) -> None:
            self.completed_status = status
            self.session["status"] = status

    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id="evt_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.RUN_STARTED,
            )
            yield AgentEvent(
                id="evt_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=terminal_event_type,
            )

    repository = FakeRepository()
    redis_state = FakeRedisState()
    monkeypatch.setattr(sessions_module, "_get_repository", lambda: repository)
    monkeypatch.setattr(sessions_module, "_get_redis_state", lambda: redis_state)
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    result = await sessions_module._run_persistent_session("ana_001")

    assert [event["type"] for event in result["events"]] == [
        AgentEventType.RUN_STARTED.value,
        terminal_event_type.value,
    ]
    assert repository.completed_status == expected_status
    assert repository.session["status"] == expected_status


@pytest.mark.asyncio
async def test_persistent_cancel_run_releases_active_run(monkeypatch):
    class FakeRedisState:
        def __init__(self) -> None:
            self.cancelled_run_id: str | None = None

        async def request_cancel(self, run_id: str) -> None:
            self.cancelled_run_id = run_id

    class FakeRepository:
        def __init__(self) -> None:
            self.session = {
                "session_id": "ana_001",
                "run_id": "run_001",
                "status": "running",
            }
            self.completed_status: str | None = None

        async def get_session(self, session_id: str):
            return self.session if session_id == self.session["session_id"] else None

        async def complete_run(self, session_id: str, run_id: str, status: str) -> None:
            self.completed_status = status
            self.session["status"] = status

    repository = FakeRepository()
    redis_state = FakeRedisState()
    monkeypatch.setattr(sessions_module, "_use_in_memory_store", lambda: False)
    monkeypatch.setattr(sessions_module, "_get_repository", lambda: repository)
    monkeypatch.setattr(sessions_module, "_get_redis_state", lambda: redis_state)

    response = await sessions_module.cancel_run("ana_001", "run_001")

    assert response == {"session_id": "ana_001", "run_id": "run_001", "status": "cancel_requested"}
    assert redis_state.cancelled_run_id == "run_001"
    assert repository.completed_status == "cancelled"
    assert repository.session["status"] == "cancelled"


@pytest.mark.asyncio
async def test_persistent_stream_session_starts_existing_created_run(monkeypatch):
    class FakeRedisState:
        async def is_cancelled(self, run_id: str) -> bool:
            return False

        async def remember_last_seq(self, session_id: str, seq: int) -> None:
            return None

    class FakeRepository:
        def __init__(self) -> None:
            self.session = {
                "session_id": "ana_001",
                "run_id": "run_001",
                "status": "running",
                "raw_input": "压力波动",
            }
            self.run = {
                "id": "run_001",
                "session_id": "ana_001",
                "status": "created",
                "input_text": "压力波动",
            }
            self.created_runs: list[str] = []
            self.started_runs: list[str] = []
            self.completed_status: str | None = None

        async def get_session(self, session_id: str):
            return self.session if session_id == self.session["session_id"] else None

        async def get_run(self, session_id: str, run_id: str):
            if session_id == self.session["session_id"] and run_id == self.run["id"]:
                return self.run
            return None

        async def create_run(self, session_id: str, run_id: str) -> None:
            self.created_runs.append(run_id)

        async def start_run(self, session_id: str, run_id: str) -> None:
            self.started_runs.append(run_id)
            self.run["status"] = "running"

        async def append_event(self, event) -> None:
            return None

        async def complete_run(self, session_id: str, run_id: str, status: str) -> None:
            self.completed_status = status
            self.run["status"] = status

        async def create_run_summary(self, **kwargs) -> dict[str, Any]:
            self.summary = kwargs
            return kwargs

        async def create_message(self, **kwargs) -> dict[str, Any]:
            self.message = kwargs
            return kwargs

    repository = FakeRepository()
    async def recall_memories(user_id: str, query: str):
        return []

    async def store_memories(user_id: str, session_id: str, run_id: str, content: str):
        return {"accepted": [], "pending": []}

    monkeypatch.setattr(sessions_module, "_get_repository", lambda: repository)
    monkeypatch.setattr(sessions_module, "_get_redis_state", lambda: FakeRedisState())
    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FastCompletedService)
    monkeypatch.setattr(sessions_module, "_recall_preference_memories", recall_memories)
    monkeypatch.setattr(sessions_module, "_store_preference_memories", store_memories)

    chunks = [
        chunk async for chunk in sessions_module._stream_persistent_session("ana_001")
    ]

    assert repository.created_runs == []
    assert repository.started_runs == ["run_001"]
    assert repository.completed_status == "completed"
    assert any("event: run_status" in chunk for chunk in chunks)


def test_in_memory_run_session_preserves_terminal_status(monkeypatch):
    class FakeService:
        def __init__(self, tool_registry) -> None:
            self.tool_registry = tool_registry

        async def run_analysis(self, session_id: str, run_id: str, raw_input: str, permissions: set[str]):
            yield AgentEvent(
                id="evt_001",
                session_id=session_id,
                run_id=run_id,
                seq=1,
                type=AgentEventType.RUN_STARTED,
            )
            yield AgentEvent(
                id="evt_002",
                session_id=session_id,
                run_id=run_id,
                seq=2,
                type=AgentEventType.AWAITING_USER,
            )

    monkeypatch.setattr(sessions_module, "AgentRuntimeService", FakeService)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"raw_input": "压力波动"}).json()
    response = client.post(f"/api/agent/sessions/{created['session_id']}/runs")

    assert response.status_code == 200
    assert [event["type"] for event in response.json()["events"]] == [
        AgentEventType.RUN_STARTED.value,
        AgentEventType.AWAITING_USER.value,
    ]
    assert sessions_module._get_session(created["session_id"])["status"] == "awaiting_user"


@pytest.mark.parametrize(
    ("events", "expected_status"),
    [
        ([], "failed"),
        ([{"type": "unexpected_terminal"}], "failed"),
    ],
)
def test_derive_run_status_treats_empty_or_unknown_tail_as_failed(events, expected_status):
    assert sessions_module._derive_run_status(events) == expected_status
