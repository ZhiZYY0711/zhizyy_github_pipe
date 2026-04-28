from types import SimpleNamespace

import pytest

from app.rag.qdrant_client import DOMAIN_KNOWLEDGE_COLLECTION, SIMILAR_CASES_COLLECTION
from app.rag.retriever import QdrantRetriever


class FakeEmbeddings:
    async def embed_query(self, text: str) -> list[float]:
        return [0.1, 0.2, 0.3]


class FakeQdrantClient:
    def __init__(self) -> None:
        self.calls: list[dict] = []

    async def search(
        self,
        collection_name: str,
        query_vector: list[float],
        limit: int,
        with_payload: bool = True,
    ) -> list[SimpleNamespace]:
        self.calls.append(
            {
                "collection_name": collection_name,
                "query_vector": query_vector,
                "limit": limit,
                "with_payload": with_payload,
            }
        )
        return [
            SimpleNamespace(
                id="point_001",
                score=0.87,
                payload={
                    "doc_id": "case_001",
                    "chunk_id": "chunk_01",
                    "title": "Cathodic protection abnormal case",
                    "summary": "Voltage drift matched a previous maintenance case.",
                },
            )
        ]


class MissingCollectionClient:
    async def query_points(
        self,
        collection_name: str,
        query: list[float],
        limit: int,
        with_payload: bool = True,
    ) -> list[SimpleNamespace]:
        raise RuntimeError(f"Not found: Collection `{collection_name}` doesn't exist!")


class BrokenQdrantClient:
    async def query_points(
        self,
        collection_name: str,
        query: list[float],
        limit: int,
        with_payload: bool = True,
    ) -> list[SimpleNamespace]:
        raise RuntimeError("connection refused")


@pytest.mark.asyncio
async def test_qdrant_retriever_returns_similar_case_citations() -> None:
    client = FakeQdrantClient()
    retriever = QdrantRetriever(client=client, embeddings=FakeEmbeddings())

    docs = await retriever.search_similar_cases("cp voltage abnormal", limit=2)

    assert client.calls[0]["collection_name"] == SIMILAR_CASES_COLLECTION
    assert client.calls[0]["query_vector"] == [0.1, 0.2, 0.3]
    assert client.calls[0]["limit"] == 2
    assert docs[0]["source"] == "similar_cases"
    assert docs[0]["citation"] == "case:case_001#chunk_01"


@pytest.mark.asyncio
async def test_qdrant_retriever_uses_domain_knowledge_collection() -> None:
    client = FakeQdrantClient()
    retriever = QdrantRetriever(client=client, embeddings=FakeEmbeddings())

    docs = await retriever.search_domain_knowledge("inspection method", limit=1)

    assert client.calls[0]["collection_name"] == DOMAIN_KNOWLEDGE_COLLECTION
    assert docs[0]["citation"] == "doc:case_001#chunk_01"


@pytest.mark.asyncio
async def test_qdrant_retriever_returns_empty_docs_when_collection_is_missing() -> None:
    retriever = QdrantRetriever(client=MissingCollectionClient(), embeddings=FakeEmbeddings())

    docs = await retriever.search_domain_knowledge("inspection method", limit=1)

    assert docs == []


@pytest.mark.asyncio
async def test_qdrant_retriever_reraises_other_search_errors() -> None:
    retriever = QdrantRetriever(client=BrokenQdrantClient(), embeddings=FakeEmbeddings())

    with pytest.raises(RuntimeError, match="connection refused"):
        await retriever.search_domain_knowledge("inspection method", limit=1)
