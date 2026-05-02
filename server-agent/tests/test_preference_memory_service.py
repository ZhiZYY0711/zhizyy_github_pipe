from app.services.preference_memory_service import (
    extract_preference_candidates,
    format_recalled_memories_for_prompt,
    rank_recalled_memories,
)


def test_extracts_explicit_low_risk_preference() -> None:
    candidates = extract_preference_candidates(
        user_id="u_1",
        session_id="ana_1",
        run_id="run_1",
        content="以后回答先给结论，别写太长",
    )

    assert len(candidates) == 1
    assert candidates[0].memory_type == "preference"
    assert candidates[0].preference_key == "answer.conclusion_first"
    assert candidates[0].risk_level == "low"
    assert candidates[0].proposed_action == "auto_accept"
    assert "先给结论" in candidates[0].content


def test_extracts_high_risk_analysis_preference_for_confirmation() -> None:
    candidates = extract_preference_candidates(
        user_id="u_1",
        session_id="ana_1",
        run_id="run_1",
        content="以后风险都按高等级处理",
    )

    assert len(candidates) == 1
    assert candidates[0].memory_type == "analysis_preference"
    assert candidates[0].risk_level == "high"
    assert candidates[0].proposed_action == "needs_confirmation"


def test_does_not_extract_one_time_business_fact() -> None:
    candidates = extract_preference_candidates(
        user_id="u_1",
        session_id="ana_1",
        run_id="run_1",
        content="CP-04 今天压力波动明显",
    )

    assert candidates == []


def test_rank_recalled_memories_prioritizes_export_when_query_mentions_report() -> None:
    memories = [
        {"memoryType": "preference", "content": "回答先给结论。"},
        {"memoryType": "export_preference", "content": "默认导出 Excel。"},
    ]

    ranked = rank_recalled_memories(memories, "帮我导出报告")

    assert ranked[0]["memoryType"] == "export_preference"


def test_format_recalled_memories_for_prompt_groups_by_type() -> None:
    text = format_recalled_memories_for_prompt(
        [
            {"memoryType": "preference", "content": "回答先给结论。"},
            {"memoryType": "interaction_preference", "content": "信息不足时先追问。"},
        ]
    )

    assert "用户长期偏好" in text
    assert "输出偏好：回答先给结论。" in text
    assert "交互偏好：信息不足时先追问。" in text
