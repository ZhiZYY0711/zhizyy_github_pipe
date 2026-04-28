from fastapi import FastAPI

from app.api.sessions import router as sessions_router
from app.core.config import get_settings
from app.logging_config import configure_file_logging


def create_app() -> FastAPI:
    settings = get_settings()
    configure_file_logging()
    app = FastAPI(title=settings.app_name)
    app.include_router(sessions_router)

    @app.get("/health")
    async def health() -> dict[str, str]:
        return {"status": "ok", "service": settings.app_name}

    return app


app = create_app()
