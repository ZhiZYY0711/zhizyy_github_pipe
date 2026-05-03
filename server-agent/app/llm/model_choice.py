from typing import Literal

from pydantic import BaseModel

from app.core.config import get_settings

ModelTier = Literal["auto", "light", "standard", "performance", "ultimate"]

MODEL_TIER_LABELS: dict[str, str] = {
    "auto": "Auto",
    "light": "轻量",
    "standard": "标准",
    "performance": "性能",
    "ultimate": "极致",
}


class ModelChoice(BaseModel):
    tier: str
    label: str
    provider: str
    model: str


def resolve_model_choice(model_tier: str | None = None) -> ModelChoice:
    settings = get_settings()
    tier = (model_tier or settings.llm_default_tier or "auto").strip().lower()
    if tier not in MODEL_TIER_LABELS:
        tier = settings.llm_default_tier.strip().lower()
    if tier not in MODEL_TIER_LABELS:
        tier = "auto"

    model_by_tier = {
        "auto": settings.llm_auto_model,
        "light": settings.llm_light_model,
        "standard": settings.llm_standard_model,
        "performance": settings.llm_performance_model,
        "ultimate": settings.llm_ultimate_model,
    }
    model = (model_by_tier.get(tier) or settings.llm_model).strip()
    return ModelChoice(
        tier=tier,
        label=MODEL_TIER_LABELS[tier],
        provider="moonshot",
        model=model,
    )
