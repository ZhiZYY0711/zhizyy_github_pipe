from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "pipeline-virtual-expert-agent"
    environment: str = "local"
    database_url: str = "postgresql+asyncpg://agent:agent@localhost:5432/pipeline_agent"
    redis_url: str = "redis://localhost:6379/2"
    qdrant_url: str = "http://localhost:6333"
    tool_base_url: str = "http://127.0.0.1:8080"
    internal_jwt_secret: str = "dev-agent-secret"
    internal_jwt_audience: str = "pipeline-agent"
    require_internal_auth: bool = False
    use_in_memory_store: bool = False
    llm_enabled: bool = False
    llm_base_url: str = "https://api.moonshot.cn/v1"
    llm_api_key: str = ""
    llm_model: str = "kimi-k2.6"
    llm_default_tier: str = "auto"
    llm_auto_model: str = "moonshot-v1-auto"
    llm_light_model: str = "moonshot-v1-8k"
    llm_standard_model: str = "moonshot-v1-32k"
    llm_performance_model: str = "kimi-k2.5"
    llm_ultimate_model: str = "kimi-k2.6"
    llm_title_model: str = "moonshot-v1-8k"
    llm_timeout_seconds: float = 60.0
    llm_temperature: float = 1.0

    model_config = SettingsConfigDict(env_prefix="AGENT_", env_file=".env", extra="ignore")

    @model_validator(mode="after")
    def validate_internal_jwt_secret(self) -> "Settings":
        if self.environment != "local" and self.internal_jwt_secret == "dev-agent-secret":
            raise ValueError(
                "AGENT_INTERNAL_JWT_SECRET must be set to a non-default value when "
                "AGENT_ENVIRONMENT is not local."
            )
        return self


def get_settings() -> Settings:
    return Settings()
