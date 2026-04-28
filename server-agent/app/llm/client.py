import json
from collections.abc import AsyncIterator
from typing import Any, Protocol, TypeVar

import httpx

from pydantic import BaseModel

from app.core.config import get_settings

T = TypeVar("T", bound=BaseModel)


class LlmClient(Protocol):
    async def complete_json(self, system_prompt: str, user_prompt: str, schema: type[T]) -> T: ...

    def stream_chat(
        self,
        system_prompt: str,
        user_prompt: str,
        response_format: dict[str, Any] | None = None,
    ) -> AsyncIterator["LlmStreamChunk"]: ...


class LlmStreamChunk(BaseModel):
    channel: str
    delta: str


class OpenAICompatibleLlmClient:
    def __init__(
        self,
        base_url: str,
        api_key: str,
        model: str,
        timeout_seconds: float = 60.0,
        temperature: float = 1.0,
        client: httpx.AsyncClient | None = None,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._api_key = api_key.strip()
        self._model = model
        self._timeout_seconds = timeout_seconds
        self._temperature = temperature
        self._client = client

    async def complete_json(self, system_prompt: str, user_prompt: str, schema: type[T]) -> T:
        schema_prompt = (
            f"{user_prompt}\n\n"
            "Return JSON only. It must validate against this JSON schema:\n"
            f"{json.dumps(schema.model_json_schema(), ensure_ascii=False)}"
        )
        payload = {
            "model": self._model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": schema_prompt},
            ],
            "temperature": self._temperature,
            "response_format": {"type": "json_object"},
        }
        response = await self._post_chat_completion(payload)
        content = _extract_message_content(response)
        return schema.model_validate_json(normalize_json_content(content))

    async def stream_chat(
        self,
        system_prompt: str,
        user_prompt: str,
        response_format: dict[str, Any] | None = None,
    ) -> AsyncIterator[LlmStreamChunk]:
        payload: dict[str, Any] = {
            "model": self._model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": self._temperature,
            "stream": True,
        }
        if response_format is not None:
            payload["response_format"] = response_format

        headers = {"Authorization": f"Bearer {self._api_key}"}
        if self._client is not None:
            response = await self._client.post(
                f"{self._base_url}/chat/completions",
                headers=headers,
                json=payload,
                timeout=self._timeout_seconds,
            )
            response.raise_for_status()
            async for chunk in _iter_sse_chunks(response.aiter_lines()):
                yield chunk
            return

        async with httpx.AsyncClient() as client:
            async with client.stream(
                "POST",
                f"{self._base_url}/chat/completions",
                headers=headers,
                json=payload,
                timeout=self._timeout_seconds,
            ) as response:
                response.raise_for_status()
                async for chunk in _iter_sse_chunks(response.aiter_lines()):
                    yield chunk

    async def _post_chat_completion(self, payload: dict[str, Any]) -> dict[str, Any]:
        headers = {"Authorization": f"Bearer {self._api_key}"}
        if self._client is not None:
            response = await self._client.post(
                f"{self._base_url}/chat/completions",
                headers=headers,
                json=payload,
                timeout=self._timeout_seconds,
            )
            response.raise_for_status()
            return response.json()

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self._base_url}/chat/completions",
                headers=headers,
                json=payload,
                timeout=self._timeout_seconds,
            )
            response.raise_for_status()
            return response.json()


def build_llm_client() -> LlmClient | None:
    settings = get_settings()
    if not settings.llm_enabled or not settings.llm_api_key.strip():
        return None
    return OpenAICompatibleLlmClient(
        base_url=settings.llm_base_url,
        api_key=settings.llm_api_key,
        model=settings.llm_model,
        timeout_seconds=settings.llm_timeout_seconds,
        temperature=settings.llm_temperature,
    )


def _extract_message_content(response: dict[str, Any]) -> str:
    choices = response.get("choices")
    if not isinstance(choices, list) or not choices:
        raise ValueError("LLM response has no choices")
    message = choices[0].get("message") if isinstance(choices[0], dict) else None
    content = message.get("content") if isinstance(message, dict) else None
    if not isinstance(content, str) or not content.strip():
        raise ValueError("LLM response content is empty")
    return content


def normalize_json_content(content: str) -> str:
    stripped = content.strip()
    if not stripped.startswith("```"):
        return stripped

    lines = stripped.splitlines()
    if len(lines) >= 2 and lines[0].strip().startswith("```"):
        if lines[-1].strip() == "```":
            lines = lines[1:-1]
        else:
            lines = lines[1:]
    return "\n".join(lines).strip()


async def _iter_sse_chunks(lines: AsyncIterator[str]) -> AsyncIterator[LlmStreamChunk]:
    async for line in lines:
        stripped = line.strip()
        if not stripped.startswith("data:"):
            continue

        data = stripped.removeprefix("data:").strip()
        if not data or data == "[DONE]":
            continue

        payload = json.loads(data)
        choices = payload.get("choices")
        if not isinstance(choices, list) or not choices:
            continue

        delta = choices[0].get("delta") if isinstance(choices[0], dict) else None
        if not isinstance(delta, dict):
            continue

        reasoning = delta.get("reasoning_content")
        if isinstance(reasoning, str) and reasoning:
            yield LlmStreamChunk(channel="reasoning_content", delta=reasoning)

        content = delta.get("content")
        if isinstance(content, str) and content:
            yield LlmStreamChunk(channel="content", delta=content)
