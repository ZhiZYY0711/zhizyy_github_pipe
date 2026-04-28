import pytest

from app.persistence.redis_state import AgentRedisState


@pytest.mark.asyncio
async def test_in_memory_redis_state_tracks_cancel_and_last_seq():
    state = AgentRedisState.in_memory()

    await state.request_cancel("run_001")
    await state.remember_last_seq("ana_001", 7)

    assert await state.is_cancelled("run_001") is True
    assert state.last_seq("ana_001") == 7
