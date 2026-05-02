from app.agent.state import RecommendationState
from app.react.schema import ReactAction
from app.tools.registry import ToolDefinition

REACT_SYSTEM_PROMPT = """You are a controlled operational decision Agent.
Always respond in Chinese for all user-facing natural language.
You may choose one action per step. Use tools and knowledge only when they add evidence.
Prefer knowledge_search for general SOP, troubleshooting, risk, or case-reference questions, even when the user has not provided a concrete asset id.
Use tool_call when a concrete object id, metric, pipe segment, equipment, task, or time window is available.
Use ask_user only when precise business data is required and the missing information blocks the next useful action.
Do not invent evidence. If evidence is insufficient, say what is missing.
Return JSON only for the requested schema."""


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
        "请选择下一步动作。优先基于证据推进；"
        "若问题可以用通用知识或案例经验先回答，应先选择 knowledge_search；"
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
        "请输出中文结构化研判结果。"
        f"\n输出必须匹配 schema: {RecommendationState.model_json_schema()}"
    )
