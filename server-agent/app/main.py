from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.logging_config import configure_file_logging, reattach_file_handler_to_root

# Configure file logging before importing routers so that any module-level
# logger creation happens after the logging system is fully initialised.
configure_file_logging()

from app.api.sessions import public_router as public_sessions_router  # noqa: E402
from app.api.sessions import router as sessions_router  # noqa: E402
from app.core.config import get_settings  # noqa: E402


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
    # uvicorn's dictConfig runs before lifespan; re-add the file handler to
    # root so uvicorn errors also land in the daily log file.
    reattach_file_handler_to_root()
    yield


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(title=settings.app_name, lifespan=lifespan)
    app.include_router(sessions_router)
    app.include_router(public_sessions_router)

    @app.get("/health")
    async def health() -> dict[str, str]:
        return {"status": "ok", "service": settings.app_name}

    return app


app = create_app()
