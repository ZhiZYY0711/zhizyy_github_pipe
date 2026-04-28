from dataclasses import dataclass
from typing import Any, Protocol
from uuid import uuid5, NAMESPACE_URL

from app.rag.embeddings import DeterministicEmbeddingProvider, EmbeddingProvider
from app.rag.qdrant_client import DOMAIN_KNOWLEDGE_COLLECTION

CASE_KNOWLEDGE_COLLECTION = "case_knowledge"
VECTOR_SIZE = 32


class SeedQdrantClient(Protocol):
    async def recreate_collection(self, collection_name: str, vectors_config: Any) -> None: ...

    async def upsert(self, collection_name: str, points: list[Any]) -> None: ...


@dataclass(frozen=True)
class SeedPoint:
    id: str
    vector: list[float]
    payload: dict[str, Any]


_DOMAIN_ITEMS = [
    {
        "doc_id": "domain_pressure_sop",
        "title": "管道压力异常处置 SOP",
        "content": "当管段压力出现连续波动时，应先校验压力传感器状态，再比对上下游流量和阀门开度。",
        "summary": "压力异常应先校验传感器，再联动查看流量、阀门和上下游变化。",
        "tags": ["pressure", "sop", "pipeline"],
        "risk_level": "medium",
    },
    {
        "doc_id": "domain_cp_voltage_rule",
        "title": "阴保电位异常判断规则",
        "content": "阴保电位短时漂移需要结合土壤湿度、测试桩状态和近期检修记录判断，避免误判为腐蚀风险。",
        "summary": "阴保电位异常需要结合环境、测试桩和检修记录综合判断。",
        "tags": ["cp_voltage", "corrosion", "rule"],
        "risk_level": "medium",
    },
    {
        "doc_id": "domain_pump_flow_drop",
        "title": "泵站流量突降排查步骤",
        "content": "泵站流量突降时，应检查泵组运行状态、入口压力、过滤器压差和下游阀室状态。",
        "summary": "流量突降优先排查泵组、入口压力、过滤器和下游阀室。",
        "tags": ["flow", "pump", "inspection"],
        "risk_level": "high",
    },
    {
        "doc_id": "domain_valve_temperature",
        "title": "阀室温度异常应急建议",
        "content": "阀室温度持续升高时，应核查通风、伴热、电气柜负载和可燃气体报警状态。",
        "summary": "阀室温度异常需要联动检查通风、伴热、电气柜和气体报警。",
        "tags": ["temperature", "valve", "emergency"],
        "risk_level": "high",
    },
    {
        "doc_id": "domain_sensor_drift",
        "title": "传感器离线和漂移判断规则",
        "content": "传感器离线或漂移应先确认通信链路、电源、采样频率和邻近传感器趋势一致性。",
        "summary": "传感器异常先判断通信、电源、采样频率和邻近趋势一致性。",
        "tags": ["sensor", "offline", "drift"],
        "risk_level": "low",
    },
]

_CASE_ITEMS = [
    {
        "doc_id": "case_pressure_001",
        "title": "压力波动相似案例",
        "content": "某输油管段压力在 40 分钟内反复波动，最终确认为压力传感器接线松动叠加阀门开度调整。",
        "summary": "压力波动案例：传感器接线松动与阀门调整共同导致异常。",
        "tags": ["pressure", "sensor", "valve"],
        "risk_level": "medium",
    },
    {
        "doc_id": "case_cp_001",
        "title": "阴保电位波动相似案例",
        "content": "雨后阴保电位出现短时波动，经复核为测试桩接触不良，未发现腐蚀扩大迹象。",
        "summary": "阴保电位波动案例：雨后测试桩接触不良造成误报。",
        "tags": ["cp_voltage", "rain", "false_positive"],
        "risk_level": "low",
    },
    {
        "doc_id": "case_flow_001",
        "title": "流量突降相似案例",
        "content": "东区泵站流量突降，工具趋势显示入口压力同步下降，现场发现过滤器堵塞。",
        "summary": "流量突降案例：入口压力下降与过滤器堵塞相关。",
        "tags": ["flow", "pump", "filter"],
        "risk_level": "high",
    },
    {
        "doc_id": "case_temperature_001",
        "title": "阀室温度异常相似案例",
        "content": "阀室温度持续升高，检查发现通风设备停转，恢复通风后温度回落。",
        "summary": "温度异常案例：通风设备停转导致阀室升温。",
        "tags": ["temperature", "valve", "ventilation"],
        "risk_level": "medium",
    },
    {
        "doc_id": "case_device_offline_001",
        "title": "设备离线误报案例",
        "content": "多个传感器同一时刻离线，排查后确认为通信网关重启，不属于设备本体故障。",
        "summary": "设备离线案例：通信网关重启造成批量离线误报。",
        "tags": ["sensor", "offline", "gateway"],
        "risk_level": "low",
    },
]


async def build_seed_points(
    collection_name: str,
    embeddings: EmbeddingProvider | None = None,
) -> list[SeedPoint]:
    provider = embeddings or DeterministicEmbeddingProvider(VECTOR_SIZE)
    items = _items_for_collection(collection_name)
    points: list[SeedPoint] = []
    for index, item in enumerate(items):
        chunk_id = "main"
        vector = await provider.embed_query(f"{item['title']}\n{item['content']}")
        payload = {
            **item,
            "chunk_id": chunk_id,
            "source_type": collection_name,
            "object_types": ["pipeline", "pipe_segment", "equipment", "sensor"],
            "seeded_demo": True,
            "metadata": {"seeded_demo": True, "seed_index": index},
        }
        points.append(
            SeedPoint(
                id=str(uuid5(NAMESPACE_URL, f"{collection_name}:{item['doc_id']}:{chunk_id}")),
                vector=vector,
                payload=payload,
            )
        )
    return points


async def seed_qdrant_collections(
    client: SeedQdrantClient | None = None,
    embeddings: EmbeddingProvider | None = None,
) -> dict[str, int]:
    qdrant_client = client or _build_qdrant_client()
    result: dict[str, int] = {}
    for collection_name in (DOMAIN_KNOWLEDGE_COLLECTION, CASE_KNOWLEDGE_COLLECTION):
        points = await build_seed_points(collection_name, embeddings)
        await qdrant_client.recreate_collection(
            collection_name=collection_name,
            vectors_config=_vectors_config(),
        )
        await qdrant_client.upsert(collection_name=collection_name, points=_qdrant_points(points))
        result[collection_name] = len(points)
    return result


def _items_for_collection(collection_name: str) -> list[dict[str, Any]]:
    if collection_name == DOMAIN_KNOWLEDGE_COLLECTION:
        return _DOMAIN_ITEMS
    if collection_name == CASE_KNOWLEDGE_COLLECTION:
        return _CASE_ITEMS
    raise ValueError(f"Unsupported seed collection: {collection_name}")


def _vectors_config() -> Any:
    try:
        from qdrant_client.models import Distance, VectorParams

        return VectorParams(size=VECTOR_SIZE, distance=Distance.COSINE)
    except Exception:
        return {"size": VECTOR_SIZE, "distance": "Cosine"}


def _qdrant_points(points: list[SeedPoint]) -> list[Any]:
    try:
        from qdrant_client.models import PointStruct

        return [
            PointStruct(id=point.id, vector=point.vector, payload=point.payload)
            for point in points
        ]
    except Exception:
        return points


def _build_qdrant_client() -> SeedQdrantClient:
    from app.rag.qdrant_client import build_async_qdrant_client

    return build_async_qdrant_client()
