from typing import Any


class AgentRedisState:
    def __init__(self, client: Any | None = None) -> None:
        self._client = client
        self._cancelled_run_ids: set[str] = set()
        self._last_seq_by_session: dict[str, int] = {}

    @classmethod
    def in_memory(cls) -> "AgentRedisState":
        return cls()

    @classmethod
    def from_url(cls, redis_url: str) -> "AgentRedisState":
        from redis import asyncio as redis

        return cls(redis.from_url(redis_url, decode_responses=True))

    async def request_cancel(self, run_id: str) -> None:
        if self._client is None:
            self._cancelled_run_ids.add(run_id)
            return
        await self._client.set(_cancel_key(run_id), "1")

    async def is_cancelled(self, run_id: str) -> bool:
        if self._client is None:
            return run_id in self._cancelled_run_ids
        return bool(await self._client.get(_cancel_key(run_id)))

    async def remember_last_seq(self, session_id: str, seq: int) -> None:
        if self._client is None:
            self._last_seq_by_session[session_id] = seq
            return
        await self._client.set(_last_seq_key(session_id), str(seq))

    def last_seq(self, session_id: str) -> int | None:
        return self._last_seq_by_session.get(session_id)


def _cancel_key(run_id: str) -> str:
    return f"agent:run:{run_id}:cancel"


def _last_seq_key(session_id: str) -> str:
    return f"agent:session:{session_id}:last_seq"
