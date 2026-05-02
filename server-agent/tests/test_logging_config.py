import logging
import subprocess
from pathlib import Path

from app.logging_config import configure_file_logging, default_log_file


WORKTREE_ROOT = Path(__file__).resolve().parents[2]


def test_default_log_file_is_under_worktree_root_logs() -> None:
    assert default_log_file() == WORKTREE_ROOT / ".logs" / "server-agent.log"


def test_configure_file_logging_writes_to_requested_file(tmp_path) -> None:
    log_file = tmp_path / "agent.log"

    configured = configure_file_logging(log_file)
    logging.getLogger("app.tests").info("hello file log")

    for handler in logging.getLogger().handlers:
        handler.flush()

    assert configured == log_file.resolve()
    assert "hello file log" in log_file.read_text(encoding="utf-8")


def test_dev_logs_script_points_services_to_worktree_root_logs() -> None:
    expected = {
        "frontend": WORKTREE_ROOT / ".logs" / "frontend-client.log",
        "server-web": WORKTREE_ROOT / ".logs" / "server-web.log",
        "agent": WORKTREE_ROOT / ".logs" / "server-agent.log",
    }

    for service, path in expected.items():
        result = subprocess.run(
            ["bash", str(WORKTREE_ROOT / "scripts" / "dev-logs.sh"), "--path", service],
            check=True,
            cwd=WORKTREE_ROOT,
            text=True,
            capture_output=True,
        )
        assert result.stdout.strip() == str(path)
