from typing import Any, Protocol

from app.rag.embeddings import DeterministicEmbeddingProvider, EmbeddingProvider
from app.rag.qdrant_client import (
    DOMAIN_KNOWLEDGE_COLLECTION,
    SIMILAR_CASES_COLLECTION,
    build_async_qdrant_client,
)


class QdrantClientLike(Protocol):
    async def search(
        self,
        collection_name: str,
        query_vector: list[float],
        limit: int,
        with_payload: bool = True,
    ) -> list[Any]: ...

    async def query_points(
        self,
        collection_name: str,
        query: list[float],
        limit: int,
        with_payload: bool = True,
    ) -> Any: ...


class QdrantRetriever:
    def __init__(
        self,
        client: QdrantClientLike,
        embeddings: EmbeddingProvider | None = None,
    ) -> None:
        self._client = client
        self._embeddings = embeddings or DeterministicEmbeddingProvider()

    async def __call__(self, query: str) -> list[dict[str, Any]]:
        domain_docs = await self.search_domain_knowledge(query, limit=3)
        case_docs = await self.search_similar_cases(query, limit=2)
        return domain_docs + case_docs

    async def search_domain_knowledge(self, query: str, limit: int = 3) -> list[dict[str, Any]]:
        return await self._search(
            collection_name=DOMAIN_KNOWLEDGE_COLLECTION,
            source="domain_knowledge",
            citation_prefix="doc",
            query=query,
            limit=limit,
        )

    async def search_similar_cases(self, query: str, limit: int = 3) -> list[dict[str, Any]]:
        return await self._search(
            collection_name=SIMILAR_CASES_COLLECTION,
            source="similar_cases",
            citation_prefix="case",
            query=query,
            limit=limit,
        )

    async def _search(
        self,
        collection_name: str,
        source: str,
        citation_prefix: str,
        query: str,
        limit: int,
    ) -> list[dict[str, Any]]:
        vector = await self._embeddings.embed_query(query)
        try:
            if hasattr(self._client, "query_points"):
                response = await self._client.query_points(
                    collection_name=collection_name,
                    query=vector,
                    limit=limit,
                    with_payload=True,
                )
                hits = getattr(response, "points", response)
            else:
                hits = await self._client.search(
                    collection_name=collection_name,
                    query_vector=vector,
                    limit=limit,
                    with_payload=True,
                )
        except Exception as exc:
            if _is_missing_collection_error(exc):
                return []
            raise
        return [self._normalize_hit(hit, source, citation_prefix) for hit in hits]

    def _normalize_hit(self, hit: Any, source: str, citation_prefix: str) -> dict[str, Any]:
        payload = dict(getattr(hit, "payload", None) or {})
        doc_id = str(payload.get("doc_id") or payload.get("id") or getattr(hit, "id", "unknown"))
        chunk_id = str(payload.get("chunk_id") or payload.get("chunk") or "main")
        return {
            "doc_id": doc_id,
            "chunk_id": chunk_id,
            "title": payload.get("title"),
            "summary": payload.get("summary") or payload.get("content"),
            "source": source,
            "score": float(getattr(hit, "score", 0)),
            "citation": f"{citation_prefix}:{doc_id}#{chunk_id}",
            "metadata": payload.get("metadata", {}),
        }


def build_qdrant_retriever() -> QdrantRetriever:
    return QdrantRetriever(client=build_async_qdrant_client())


def _is_missing_collection_error(exc: Exception) -> bool:
    message = str(exc)
    return "Not found: Collection" in message or "doesn't exist" in message
