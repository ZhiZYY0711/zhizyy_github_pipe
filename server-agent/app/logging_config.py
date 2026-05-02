import logging
import os
from pathlib import Path


DEFAULT_LOG_FORMAT = "%(asctime)s [%(levelname)s] %(name)s - %(message)s"


def default_log_file() -> Path:
    return Path(__file__).resolve().parents[2] / ".logs" / "server-agent.log"


def configure_file_logging(log_file: str | Path | None = None) -> Path:
    target = Path(log_file or os.getenv("AGENT_LOG_FILE") or default_log_file())
    target.parent.mkdir(parents=True, exist_ok=True)

    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    resolved_target = target.resolve()
    for handler in root_logger.handlers:
        if isinstance(handler, logging.FileHandler) and Path(handler.baseFilename) == resolved_target:
            return resolved_target

    file_handler = logging.FileHandler(resolved_target, encoding="utf-8")
    file_handler.setFormatter(logging.Formatter(DEFAULT_LOG_FORMAT))
    root_logger.addHandler(file_handler)
    logging.getLogger("app").info("file logging configured: %s", resolved_target)
    return resolved_target
