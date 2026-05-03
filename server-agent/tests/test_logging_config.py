import logging
from pathlib import Path

from app.logging_config import configure_file_logging, default_log_dir


WORKTREE_ROOT = Path(__file__).resolve().parents[2]


def test_default_log_dir_is_under_worktree_root_logs() -> None:
    assert default_log_dir() == WORKTREE_ROOT / ".logs"


def test_configure_file_logging_creates_handler(tmp_path) -> None:
    log_dir = tmp_path / "logs"

    configured = configure_file_logging(log_dir)
    logging.getLogger("app.tests").info("hello file log")

    for handler in logging.getLogger().handlers:
        handler.flush()

    assert configured == log_dir
    today_dir = log_dir / __import__("datetime").date.today().isoformat()
    assert (today_dir / "server-agent.log").exists()
    assert "hello file log" in (today_dir / "server-agent.log").read_text(encoding="utf-8")
