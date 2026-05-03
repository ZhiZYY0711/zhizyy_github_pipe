import re

from app.core.config import get_settings
from app.llm.client import build_llm_client


TITLE_SYSTEM_PROMPT = "你是管网虚拟专家的会话标题生成器，只输出一个简短中文标题。"


def fallback_title(text: str, limit: int = 36) -> str:
    normalized = re.sub(r"\s+", " ", text).strip()
    return (normalized or "新研判会话")[:limit]


async def generate_session_title(text: str) -> str:
    settings = get_settings()
    client = build_llm_client(model_override=settings.llm_title_model)
    if client is None:
        return fallback_title(text)
    prompt = (
        "请为下面的管网研判对话生成标题。\n"
        "要求：中文，8到18字，不要使用句号、问号、引号，不要包含“请问”“帮我”“分析一下”。\n"
        f"用户输入：{text}"
    )
    try:
        title = await client.complete_text(TITLE_SYSTEM_PROMPT, prompt)
    except Exception:
        return fallback_title(text)
    return clean_generated_title(title) or fallback_title(text)


def clean_generated_title(title: str) -> str:
    cleaned = title.strip().strip("`#*_\"'“”‘’")
    cleaned = re.sub(r"^(标题|会话标题)\s*[：:]\s*", "", cleaned)
    cleaned = re.sub(r"[。？！?！]+$", "", cleaned)
    cleaned = re.sub(r"\s+", "", cleaned)
    return cleaned[:24]
