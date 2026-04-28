import logging

from app.logging_config import configure_file_logging


def test_configure_file_logging_writes_to_requested_file(tmp_path) -> None:
    log_file = tmp_path / "agent.log"

    configured = configure_file_logging(log_file)
    logging.getLogger("app.tests").info("hello file log")

    for handler in logging.getLogger().handlers:
        handler.flush()

    assert configured == log_file.resolve()
    assert "hello file log" in log_file.read_text(encoding="utf-8")
