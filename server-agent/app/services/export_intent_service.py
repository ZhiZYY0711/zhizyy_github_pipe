import logging
from collections.abc import Callable
from typing import Any, Literal

from pydantic import BaseModel, Field

logger = logging.getLogger(__name__)


class ExportPlanSection(BaseModel):
    type: str
    title: str
    content_policy: str = "standard"


class ExportPlanTable(BaseModel):
    type: str
    title: str


class ExportIntentPlan(BaseModel):
    should_export: bool = False
    format: Literal["pdf", "excel"] = "pdf"
    scope: Literal["agent_selected", "latest_turn", "full_session", "all_conversations"] = "agent_selected"
    title: str = "虚拟专家研判报告"
    style: str = "freeform_report"
    audience: str = "现场处置人员"
    purpose: str = "研判归档"
    tone: str = "清晰直接"
    detail_level: str = "standard"
    sections: list[ExportPlanSection] = Field(default_factory=list)
    tables: list[ExportPlanTable] = Field(default_factory=list)
    evidence_policy: str = "llm_selected"
    include_evidence: bool = True
    include_timeline: bool = True
    requires_confirmation: bool = True
    reason: str = ""


EXPORT_INTENT_SYSTEM_PROMPT = """你是虚拟专家 Agent 的导出意图规划器。
你的职责不是做关键词匹配，而是理解用户是否在要求平台生成 PDF/Excel 等文件。
如果用户是在继续业务研判、提问、查询数据或讨论“导出能力本身”，should_export 必须为 false。
如果用户确实希望生成文件，由你决定 format、scope、版式、受众、章节、表格、证据策略和是否需要确认。
scope 只能取 agent_selected、latest_turn、full_session、all_conversations。
不要编造已导出的文件路径；这里只生成导出计划。
所有面向用户的文字使用中文。返回 JSON。"""


async def infer_export_plan(
    content: str,
    *,
    llm_client_factory: Callable[[], Any],
) -> dict[str, Any] | None:
    llm_client = llm_client_factory()
    if llm_client is None:
        return None
    try:
        intent = await llm_client.complete_json(
            system_prompt=EXPORT_INTENT_SYSTEM_PROMPT,
            user_prompt=f"用户消息：{content}",
            schema=ExportIntentPlan,
        )
    except Exception:
        logger.exception("export intent planning failed")
        return None
    if not intent.should_export:
        return None
    return export_plan_from_intent(intent)


def export_plan_from_intent(intent: ExportIntentPlan) -> dict[str, Any]:
    return {
        "format": intent.format,
        "scope": intent.scope,
        "title": intent.title,
        "style": intent.style,
        "audience": intent.audience,
        "purpose": intent.purpose,
        "tone": intent.tone,
        "detailLevel": intent.detail_level,
        "sections": [
            {"type": section.type, "title": section.title, "contentPolicy": section.content_policy}
            for section in intent.sections
        ],
        "tables": [{"type": table.type, "title": table.title} for table in intent.tables],
        "evidencePolicy": intent.evidence_policy,
        "includeEvidence": intent.include_evidence,
        "includeTimeline": intent.include_timeline,
        "requiresConfirmation": intent.requires_confirmation,
        "reason": intent.reason or "LLM 已生成导出计划。",
    }
