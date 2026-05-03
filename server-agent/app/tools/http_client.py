from typing import Any

import httpx
from jose import jwt

from app.core.config import get_settings


class ToolHttpClient:
    def __init__(self, base_url: str, client: httpx.AsyncClient | None = None) -> None:
        self._base_url = base_url.rstrip("/")
        self._client = client

    async def get(self, path: str, params: dict[str, Any]) -> dict[str, Any]:
        return await self._request("GET", path, params=params)

    async def post(self, path: str, payload: dict[str, Any]) -> dict[str, Any]:
        return await self._request("POST", path, json=payload)

    async def _request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, Any] | None = None,
        json: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        settings = get_settings()
        headers = {
            "Authorization": f"Bearer {_internal_token()}",
            "X-Agent-Audience": settings.internal_jwt_audience,
        }
        client = self._client or httpx.AsyncClient()
        close_client = self._client is None
        try:
            response = await client.request(
                method,
                f"{self._base_url}{path}",
                params={key: value for key, value in (params or {}).items() if value is not None},
                json={key: value for key, value in (json or {}).items() if value is not None} if json is not None else None,
                headers=headers,
            )
            response.raise_for_status()
            return response.json()
        finally:
            if close_client:
                await client.aclose()


def build_tool_http_client() -> ToolHttpClient:
    return ToolHttpClient(get_settings().tool_base_url)


def _internal_token() -> str:
    settings = get_settings()
    return jwt.encode(
        {"sub": "server-agent", "aud": settings.internal_jwt_audience},
        settings.internal_jwt_secret,
        algorithm="HS256",
    )
