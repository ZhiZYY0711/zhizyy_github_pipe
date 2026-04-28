from app.rag.seed import build_seed_points, seed_qdrant_collections


class FakeQdrantSeedClient:
    def __init__(self) -> None:
        self.collections: list[dict] = []
        self.upserts: list[dict] = []

    async def recreate_collection(self, collection_name: str, vectors_config: dict) -> None:
        self.collections.append({"name": collection_name, "vectors_config": vectors_config})

    async def upsert(self, collection_name: str, points: list) -> None:
        self.upserts.append({"name": collection_name, "points": points})


async def test_build_seed_points_marks_demo_payloads_and_vectors() -> None:
    points = await build_seed_points("domain_knowledge")

    assert len(points) >= 5
    first = points[0]
    assert first.vector
    assert first.payload["seeded_demo"] is True
    assert first.payload["doc_id"]
    assert first.payload["summary"]


async def test_seed_qdrant_collections_creates_domain_and_case_data() -> None:
    client = FakeQdrantSeedClient()

    result = await seed_qdrant_collections(client=client)

    assert result["domain_knowledge"] >= 5
    assert result["case_knowledge"] >= 5
    assert {item["name"] for item in client.collections} == {"domain_knowledge", "case_knowledge"}
    assert {item["name"] for item in client.upserts} == {"domain_knowledge", "case_knowledge"}
