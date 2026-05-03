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
    {
        "doc_id": "domain_pressure_drop_isolation",
        "title": "管段压力突降隔离研判要点",
        "content": "管段压力突降应先确认上下游压力是否同步变化，再核查阀门误关、泵站启停、泄漏告警和近期施工记录。若压力突降伴随流量异常增大，应优先按泄漏风险升级处置。",
        "summary": "压力突降需要联动上下游压力、阀门、泵站、泄漏告警和施工记录判断。",
        "tags": ["pressure", "isolation", "leakage", "valve"],
        "risk_level": "high",
    },
    {
        "doc_id": "domain_flow_pressure_correlation",
        "title": "流量与压力联动异常判断",
        "content": "流量下降且压力升高通常提示下游阻塞或阀门开度不足；流量上升且压力下降需要关注泄漏、旁通开启或计量偏差；单指标异常时应优先复核传感器状态。",
        "summary": "流量和压力的组合变化可区分阻塞、泄漏、旁通和传感器偏差。",
        "tags": ["flow", "pressure", "correlation", "diagnosis"],
        "risk_level": "medium",
    },
    {
        "doc_id": "domain_third_party_construction",
        "title": "第三方施工影响排查清单",
        "content": "发现第三方施工风险时，应核对施工许可、管线定位、现场监护、告警时间线和最近巡检照片。对高后果区管段，应要求现场复核并保持调度联络。",
        "summary": "第三方施工风险需要核对许可、定位、监护、告警和巡检照片。",
        "tags": ["construction", "inspection", "risk", "pipeline"],
        "risk_level": "high",
    },
    {
        "doc_id": "domain_valve_remote_operation",
        "title": "远程阀门操作前置核查",
        "content": "远程开闭阀前应确认阀门状态反馈、上下游压力差、现场作业票、联锁条件和通信质量。状态反馈不一致时，不应仅凭单一遥测值执行动作。",
        "summary": "远程阀门动作前要确认状态反馈、压差、作业票、联锁和通信质量。",
        "tags": ["valve", "remote_operation", "safety", "checklist"],
        "risk_level": "high",
    },
    {
        "doc_id": "domain_report_export_policy",
        "title": "虚拟专家报告导出内容规范",
        "content": "导出研判报告应包含用户问题、风险判断、关键证据、处置建议和待补充信息。面向领导汇报时应减少过程细节，面向现场处置时应保留证据和操作顺序。",
        "summary": "导出报告应按受众调整证据、过程和处置动作的详略。",
        "tags": ["export", "report", "evidence", "audience"],
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
    {
        "doc_id": "case_pressure_drop_002",
        "title": "压力突降与阀门误关案例",
        "content": "西线 7 号管段压力在 15 分钟内下降 18%，流量同步下降。现场复核发现下游阀室检修后阀门未完全复位，恢复开度后压力回稳。",
        "summary": "压力和流量同步下降案例：下游阀门未复位造成输送受限。",
        "tags": ["pressure", "flow", "valve", "maintenance"],
        "risk_level": "medium",
    },
    {
        "doc_id": "case_leakage_003",
        "title": "压力下降伴流量升高泄漏案例",
        "content": "城区支线夜间压力下降且瞬时流量升高，巡检发现第三方施工点附近土壤湿润并有异味，按泄漏预案隔离后确认外力损伤。",
        "summary": "压力下降伴流量升高案例：第三方施工导致泄漏风险升级。",
        "tags": ["leakage", "construction", "pressure", "flow"],
        "risk_level": "critical",
    },
    {
        "doc_id": "case_valve_feedback_001",
        "title": "阀门遥测反馈不一致案例",
        "content": "调度准备远程关阀时，阀位反馈与现场视频不一致。暂停操作后发现阀位传感器卡滞，避免了一次误操作。",
        "summary": "阀门反馈不一致案例：暂停远程动作并现场核验避免误操作。",
        "tags": ["valve", "remote_operation", "sensor", "safety"],
        "risk_level": "high",
    },
    {
        "doc_id": "case_report_export_001",
        "title": "现场处置报告导出案例",
        "content": "一次压力异常研判结束后，值班员导出 PDF 给现场人员。报告保留了压力趋势、阀门检查顺序和待补充信息，现场按清单完成复核。",
        "summary": "现场处置导出案例：PDF 保留证据和操作顺序，便于现场执行。",
        "tags": ["export", "pdf", "field_response", "pressure"],
        "risk_level": "low",
    },
    {
        "doc_id": "case_alarm_closure_001",
        "title": "告警闭环延迟案例",
        "content": "某高压告警已恢复正常但工单未闭环，复盘发现班组交接遗漏。后续要求报告中列出未闭环任务和责任人。",
        "summary": "告警闭环案例：恢复正常不等于流程闭环，需要同步任务状态。",
        "tags": ["alarm", "task", "handover", "report"],
        "risk_level": "medium",
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
