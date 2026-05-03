import logging
import os
import shutil
from logging.handlers import BaseRotatingHandler
from pathlib import Path
from datetime import datetime, timedelta

DEFAULT_LOG_FORMAT = "%(asctime)s %(levelname)-9s %(name)s - %(message)s"
_MAX_KEEP_DAYS = 30


def default_log_dir() -> Path:
    return Path(__file__).resolve().parents[2] / ".logs"


class DailyDirectoryRotatingHandler(BaseRotatingHandler):
    """Rotates log files into daily subdirectories.

    Active file:  {base}/{yyyy-MM-dd}/server-agent.log
    """

    def __init__(self, base_dir: Path, filename: str = "server-agent.log"):
        self.base_dir = base_dir
        self.filename = filename
        self._current_date = datetime.now().strftime("%Y-%m-%d")
        day_dir = base_dir / self._current_date
        day_dir.mkdir(parents=True, exist_ok=True)
        super().__init__(day_dir / filename, "a", encoding="utf-8")

    def shouldRollover(self, record):
        today = datetime.now().strftime("%Y-%m-%d")
        return today != self._current_date

    def doRollover(self):
        self.stream.close()
        self._current_date = datetime.now().strftime("%Y-%m-%d")
        day_dir = self.base_dir / self._current_date
        day_dir.mkdir(parents=True, exist_ok=True)
        self.baseFilename = str(day_dir / self.filename)
        self.stream = self._open()
        self._cleanup_old()

    def _cleanup_old(self):
        cutoff = datetime.now() - timedelta(days=_MAX_KEEP_DAYS)
        for entry in self.base_dir.iterdir():
            if entry.is_dir():
                try:
                    dir_date = datetime.strptime(entry.name, "%Y-%m-%d")
                    if dir_date < cutoff:
                        shutil.rmtree(entry)
                except ValueError:
                    pass


def configure_file_logging(log_dir: str | Path | None = None) -> Path:
    target_dir = Path(log_dir or os.getenv("AGENT_LOG_DIR") or default_log_dir())
    target_dir.mkdir(parents=True, exist_ok=True)

    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    has_file = any(isinstance(h, DailyDirectoryRotatingHandler) for h in root_logger.handlers)
    has_console = any(isinstance(h, logging.StreamHandler) and not isinstance(h, logging.FileHandler) for h in root_logger.handlers)

    if not has_file:
        file_handler = DailyDirectoryRotatingHandler(target_dir)
        file_handler.setFormatter(logging.Formatter(DEFAULT_LOG_FORMAT))
        root_logger.addHandler(file_handler)

    if not has_console:
        console_handler = logging.StreamHandler()
        console_handler.setFormatter(logging.Formatter(DEFAULT_LOG_FORMAT))
        root_logger.addHandler(console_handler)

    logging.getLogger("app").info("file logging configured: %s", target_dir)
    return target_dir
