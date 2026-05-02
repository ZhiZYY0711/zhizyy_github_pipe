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


class FakeExportPlannerLlm:
    def __init__(self, intent: dict[str, Any]) -> None:
        self.intent = intent
        self.calls: list[dict[str, str]] = []

    async def complete_json(self, system_prompt: str, user_prompt: str, schema: type) -> Any:
        self.calls.append({"system": system_prompt, "user": user_prompt})
        return schema.model_validate(self.intent)


def _planner_intent(**overrides: Any) -> dict[str, Any]:
    intent: dict[str, Any] = {
        "should_export": True,
        "format": "pdf",
        "scope": "agent_selected",
        "title": "虚拟专家研判报告",
        "style": "standard_report",
        "audience": "现场处置人员",
        "purpose": "研判归档",
        "tone": "清晰直接",
        "detail_level": "standard",
        "sections": [{"type": "summary", "title": "研判摘要", "content_policy": "concise"}],
        "tables": [{"type": "risk_list", "title": "风险清单"}],
        "evidence_policy": "include_key_evidence",
        "include_evidence": True,
        "include_timeline": True,
        "requires_confirmation": False,
        "reason": "LLM 识别到用户需要导出。",
    }
    intent.update(overrides)
    return intent


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


def test_post_message_uses_llm_export_plan_without_run(monkeypatch):
    planner = FakeExportPlannerLlm(
        _planner_intent(
            format="excel",
            scope="all_conversations",
            title="全量会话复盘表",
            style="freeform_workbook",
            audience="调度中心",
            purpose="复盘分析",
            tone="专业克制",
            detail_level="detailed",
            tables=[{"type": "dialogue_timeline", "title": "对话时间线"}],
            requires_confirmation=True,
            reason="用户要求把上述全部内容导出，LLM 决定生成全量 Excel。",
        )
    )
    monkeypatch.setattr(sessions_module, "build_llm_client", lambda: planner)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "把上述全部内容导出"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["run"] is None
    assert body["export_plan"]["format"] == "excel"
    assert body["export_plan"]["scope"] == "all_conversations"
    assert body["export_plan"]["title"] == "全量会话复盘表"
    assert body["export_plan"]["tables"] == [{"type": "dialogue_timeline", "title": "对话时间线"}]
    assert planner.calls
    assert "把上述全部内容导出" in planner.calls[0]["user"]


def test_export_words_do_not_trigger_plan_when_llm_declines(monkeypatch):
    planner = FakeExportPlannerLlm({"should_export": False, "reason": "这是业务研判，不是导出任务。"})
    monkeypatch.setattr(sessions_module, "build_llm_client", lambda: planner)

    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "请导出 PDF，但先分析一下这是不是异常"},
    )

    assert response.status_code == 200
    body = response.json()
    assert "export_plan" not in body
    assert body["run"]["status"] == "created"


def test_simple_export_plan_includes_scope_and_does_not_require_confirmation(monkeypatch):
    monkeypatch.setattr(
        sessions_module,
        "build_llm_client",
        lambda: FakeExportPlannerLlm(_planner_intent(scope="latest_turn")),
    )
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "把这次研判导出 PDF"},
    )

    assert response.status_code == 200
    plan = response.json()["export_plan"]
    assert response.json()["run"] is None
    assert plan["format"] == "pdf"
    assert plan["scope"] == "latest_turn"
    assert plan["style"] == "standard_report"
    assert plan["requiresConfirmation"] is False
    assert plan["sections"]
    assert plan["tables"]


def test_post_message_marks_complex_export_plan_for_confirmation(monkeypatch):
    monkeypatch.setattr(
        sessions_module,
        "build_llm_client",
        lambda: FakeExportPlannerLlm(
            _planner_intent(
                audience="领导汇报",
                purpose="管理汇报",
                include_evidence=False,
                evidence_policy="exclude_evidence",
                requires_confirmation=True,
            )
        ),
    )
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "给领导整理一份正式汇报 PDF，删掉证据链"},
    )

    assert response.status_code == 200
    plan = response.json()["export_plan"]
    assert plan["audience"] == "领导汇报"
    assert plan["includeEvidence"] is False
    assert plan["requiresConfirmation"] is True


def test_complex_excel_export_plan_captures_sheet_intent(monkeypatch):
    monkeypatch.setattr(
        sessions_module,
        "build_llm_client",
        lambda: FakeExportPlannerLlm(
            _planner_intent(
                format="excel",
                audience="领导汇报",
                requires_confirmation=True,
                tables=[
                    {"type": "risk_list", "title": "风险清单"},
                    {"type": "action_list", "title": "处置建议"},
                ],
            )
        ),
    )
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "Excel 分 sheet 展示风险清单和处置建议，给领导看"},
    )

    assert response.status_code == 200
    plan = response.json()["export_plan"]
    assert plan["format"] == "excel"
    assert plan["audience"] == "领导汇报"
    assert plan["requiresConfirmation"] is True
    assert [table["type"] for table in plan["tables"]] == ["risk_list", "action_list"]


def test_all_conversations_export_plan_uses_global_scope(monkeypatch):
    monkeypatch.setattr(
        sessions_module,
        "build_llm_client",
        lambda: FakeExportPlannerLlm(_planner_intent(scope="all_conversations", requires_confirmation=True)),
    )
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "请导出全部对话的 PDF"},
    )

    assert response.status_code == 200
    plan = response.json()["export_plan"]
    assert plan["format"] == "pdf"
    assert plan["scope"] == "all_conversations"
    assert plan["requiresConfirmation"] is True


def test_full_session_export_plan_keeps_current_session_scope(monkeypatch):
    monkeypatch.setattr(
        sessions_module,
        "build_llm_client",
        lambda: FakeExportPlannerLlm(_planner_intent(scope="full_session")),
    )
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "请导出整个会话 PDF"},
    )

    assert response.status_code == 200
    assert response.json()["export_plan"]["scope"] == "full_session"


def test_export_all_above_content_defaults_to_full_session_pdf(monkeypatch):
    monkeypatch.setattr(
        sessions_module,
        "build_llm_client",
        lambda: FakeExportPlannerLlm(_planner_intent(scope="full_session", requires_confirmation=True)),
    )
    client = TestClient(app)
    created = client.post("/api/agent/sessions", json={"title": "导出会话"}).json()

    response = client.post(
        f"/api/agent/sessions/{created['session_id']}/messages",
        json={"content": "把上述全部内容导出"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["run"] is None
    plan = body["export_plan"]
    assert plan["format"] == "pdf"
    assert plan["scope"] == "full_session"
    assert plan["requiresConfirmation"] is True


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


def test_cross_session_accepted_memory_is_injected_into_new_run(monkeypatch):
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

    client = TestClient(app)
    first = client.post("/api/agent/sessions", json={"title": "记忆来源"}).json()
    first_message = client.post(
        f"/api/agent/sessions/{first['session_id']}/messages",
        json={"content": "记住 CP-04 是重点管段，以后分析要优先提醒"},
    ).json()
    with client.stream(
        "GET",
        f"/api/agent/sessions/{first['session_id']}/runs/{first_message['run']['id']}/stream",
    ) as response:
        response.read()

    second = client.post("/api/agent/sessions", json={"title": "新会话"}).json()
    second_message = client.post(
        f"/api/agent/sessions/{second['session_id']}/messages",
        json={"content": "CP-04 压力波动怎么处理"},
    ).json()
    with client.stream(
        "GET",
        f"/api/agent/sessions/{second['session_id']}/runs/{second_message['run']['id']}/stream",
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
