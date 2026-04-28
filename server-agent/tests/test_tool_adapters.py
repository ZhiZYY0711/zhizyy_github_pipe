import httpx
import pytest
from jose import jwt

from app.core.config import get_settings
from app.tools.business_tools import query_monitoring_trend
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
