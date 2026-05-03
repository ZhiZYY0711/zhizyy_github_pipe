from app.llm.model_choice import resolve_model_choice


def test_resolve_model_choice_maps_auto_to_official_auto_model() -> None:
    choice = resolve_model_choice("auto")

    assert choice.tier == "auto"
    assert choice.model == "moonshot-v1-auto"
    assert choice.provider == "moonshot"


def test_resolve_model_choice_maps_five_product_tiers() -> None:
    assert resolve_model_choice("light").model == "moonshot-v1-8k"
    assert resolve_model_choice("standard").model == "moonshot-v1-32k"
    assert resolve_model_choice("performance").model == "kimi-k2.5"
    assert resolve_model_choice("ultimate").model == "kimi-k2.6"


def test_resolve_model_choice_falls_back_to_default_tier() -> None:
    choice = resolve_model_choice("unknown")

    assert choice.tier == "auto"
    assert choice.model == "moonshot-v1-auto"
