from hashlib import sha256
from typing import Protocol


class EmbeddingProvider(Protocol):
    async def embed_query(self, text: str) -> list[float]: ...


class DeterministicEmbeddingProvider:
    def __init__(self, dimensions: int = 32) -> None:
        self._dimensions = dimensions

    async def embed_query(self, text: str) -> list[float]:
        seed = sha256(text.encode("utf-8")).digest()
        values: list[float] = []
        while len(values) < self._dimensions:
            for item in seed:
                values.append((item / 127.5) - 1)
                if len(values) == self._dimensions:
                    break
            seed = sha256(seed).digest()
        return values
