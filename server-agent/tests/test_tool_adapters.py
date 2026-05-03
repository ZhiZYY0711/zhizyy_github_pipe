import httpx
import pytest
from jose import jwt

from app.core.config import get_settings
from app.tools.business_tools import (
    create_export_report,
    query_area_data_catalog,
    query_area_operational_overview,
    query_monitoring_trend,
    resolve_area_scope,
)
from app.tools.http_client import ToolHttpClient


@pytest.mark.asyncio
async def test_query_monitoring_trend_normalizes_business_response() -> None:
    async def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/internal/virtual-expert/tools/monitoring-trend"
        token = request.headers["Authorization"].removeprefix("Bearer ")
        claims = jwt.decode(
            token,
            get_settings().internal_jwt_secret,
            algorithms=["HS256"],
            audience=get_settings().internal_jwt_audience,
        )
        assert claims["sub"] == "server-agent"
        return httpx.Response(
            200,
            json={
                "metric": "cp_voltage",
                "window": "24h",
                "points": [{"time": "2026-04-25T00:00:00Z", "value": -1.22}],
                "summary": "Last 24 hours keeps fluctuating.",
            },
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as async_client:
        result = await query_monitoring_trend(
            {"object_id": "segment_001", "metric": "cp_voltage"},
            client=ToolHttpClient("http://server-web.local", async_client),
        )

    assert result["tool_name"] == "query_monitoring_trend"
    assert result["summary"] == "Last 24 hours keeps fluctuating."
    assert result["displayHint"] == "query_monitoring_trend"
    assert result["input"]["object_id"] == "segment_001"
    assert result["facts"][0]["metric"] == "cp_voltage"
    assert result["facts"][0]["label"] == "cp_voltage"
    assert result["facts"][0]["value"] == "-1.22"
    assert result["facts"][0]["points"][0]["value"] == -1.22
    assert result["raw_ref"]["source"] == "server-web"
    assert result["raw"]["metric"] == "cp_voltage"


@pytest.mark.asyncio
async def test_create_export_report_prepares_deferred_export_plan() -> None:
    result = await create_export_report(
        {
            "format": "excel",
            "scope": "full_session",
            "title": "全会话复盘表",
            "include_evidence": "false",
        }
    )

    assert result["tool_name"] == "create_export_report"
    assert result["context"]["deferred_until_run_completed"] is True
    assert result["context"]["export_plan"]["format"] == "excel"
    assert result["context"]["export_plan"]["scope"] == "full_session"
    assert result["context"]["export_plan"]["includeEvidence"] is False
    assert result["metadata"]["export_requested"] is True


@pytest.mark.asyncio
async def test_area_discovery_tools_normalize_backend_responses() -> None:
    seen_paths: list[str] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        seen_paths.append(request.url.path)
        if request.url.path.endswith("/resolve-area-scope"):
            return httpx.Response(
                200,
                json={
                    "summary": "已定位山东省济南市",
                    "context": {"resolved_scope_id": "370100", "resolved_scope_type": "city"},
                    "records": [{"scope_type": "city", "area_id": "370100", "city_name": "济南市"}],
                },
            )
        if request.url.path.endswith("/area-data-catalog"):
            return httpx.Response(
                200,
                json={
                    "summary": "济南市可查监测、告警、巡检、设备数据。",
                    "facts": [{"label": "管段数量", "value": 12}],
                },
            )
        return httpx.Response(
            200,
            json={
                "summary": "济南市运行总体平稳，存在 2 条异常监测记录。",
                "records": [{"record_type": "area_overview", "danger_count": 2}],
            },
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as async_client:
        client = ToolHttpClient("http://server-web.local", async_client)
        scope = await resolve_area_scope({"raw_input": "山东济南数据", "province": "山东", "city": "济南"}, client=client)
        catalog = await query_area_data_catalog({"area_id": "370100", "scope_type": "city"}, client=client)
        overview = await query_area_operational_overview({"area_id": "370100", "time_range": "24h"}, client=client)

    assert scope["tool_name"] == "resolve_area_scope"
    assert scope["context"]["resolved_scope_id"] == "370100"
    assert catalog["tool_name"] == "query_area_data_catalog"
    assert catalog["facts"][0]["label"] == "管段数量"
    assert overview["tool_name"] == "query_area_operational_overview"
    assert seen_paths == [
        "/internal/virtual-expert/tools/resolve-area-scope",
        "/internal/virtual-expert/tools/area-data-catalog",
        "/internal/virtual-expert/tools/area-operational-overview",
    ]
