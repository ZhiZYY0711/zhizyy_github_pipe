from app.react.schema import ReactAction
from app.tools.registry import ToolDefinition

REACT_SYSTEM_PROMPT = """You are a controlled operational decision Agent.
Always respond in Chinese for all user-facing natural language.
You may choose one action per step, or use actions[] for up to 3 sequential actions in the same step when the next useful calls are obvious.
Prefer knowledge_search for general SOP, troubleshooting, risk, or case-reference questions, even when the user has not provided a concrete asset id.
Use tool_call when a concrete object id, metric, pipe segment, equipment, task, or time window is available.
For broad location queries like "查山东济南的数据", first resolve_area_scope, then query_area_data_catalog or query_area_operational_overview before asking follow-up questions.
Use create_export_report through tool_call when the user asks to export, download, or generate a PDF/Excel/TXT/Markdown/DOCX/report.
Use ask_user only when no useful discovery, overview, knowledge, or read-only tool can make progress.
Do not invent evidence. If evidence is insufficient, say what is missing.
Return JSON only when the prompt asks for an action schema. For final answers, return Markdown only."""


def build_action_prompt(
    *,
    raw_input: str,
    step: int,
    available_tools: list[ToolDefinition],
    observations: list[dict],
    retrieved_knowledge: list[dict],
    summary_memory: list[str],
    limits: dict,
    preference_memory: list[str] | None = None,
) -> str:
    tools = [
        {
            "name": tool.name,
            "category": tool.category,
            "description": tool.description,
            "when_to_use": tool.when_to_use,
            "input_schema": tool.input_schema,
            "output_schema": tool.output_schema,
            "examples": tool.examples,
            "permission": tool.permission,
            "read_only": not tool.requires_confirmation,
        }
        for tool in available_tools
    ]
    return (
        f"用户输入: {raw_input}\n"
        f"当前步骤: {step}\n"
        f"可用工具: {tools}\n"
        f"已观察结果: {observations}\n"
        f"已检索知识: {retrieved_knowledge}\n"
        f"摘要记忆: {summary_memory}\n"
        f"用户长期偏好: {preference_memory or []}\n"
        f"运行限制: {limits}\n"
        "请选择下一步动作；如果用户意图需要明显连续步骤，可使用 actions 数组一次给出最多 3 个顺序动作。优先基于证据推进；"
        "若问题可以用通用知识或案例经验先回答，应先选择 knowledge_search；"
        "若问题是宽泛地域数据查询，应先解析区域并查询数据目录或区域总览；"
        "证据足够时选择 final_answer。"
        f"\n输出必须匹配 schema: {ReactAction.model_json_schema()}"
    )


def build_final_prompt(
    *,
    raw_input: str,
    observations: list[dict],
    retrieved_knowledge: list[dict],
    tool_calls: list[str],
    missing_information: list[str],
) -> str:
    return (
        f"用户输入: {raw_input}\n"
        f"工具与观察: {observations}\n"
        f"知识命中: {retrieved_knowledge}\n"
        f"已调用工具: {tool_calls}\n"
        f"缺失信息: {missing_information}\n"
        "请直接输出中文 Markdown 最终回答，不要输出 JSON，不要用代码块包裹全文。\n"
        "回答要结论先行、简短克制，优先控制在 3 到 5 个短段落或列表项内。\n"
        "只展示最关键的真实 facts、records、context 和知识命中，不展开内部推理过程。\n"
        "如果是宽泛数据查询，简要列出已定位范围、可查数据、关键概览和 1 到 2 个可继续下钻方向。\n"
        "如果是风险研判，简要给出风险、关键依据和下一步建议；不要强制固定条数。\n"
        "不得编造工具结果中不存在的指标或数值；缺失数据可以说明未返回。"
    )
