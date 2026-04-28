import httpx

from app.agent.state import RecommendationState
from app.llm.client import OpenAICompatibleLlmClient


async def test_openai_compatible_llm_client_posts_chat_completion_and_parses_json() -> None:
    requests: list[httpx.Request] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return httpx.Response(
            200,
            json={
                "choices": [
                    {
                        "message": {
                            "content": (
                                '{"summary":"ok","risk_level":"medium","judgment":"needs review",'
                                '"recommended_actions":["check"],"missing_information":[],'
                                '"human_confirmation_required":true}'
                            )
                        }
                    }
                ]
            },
        )

    transport = httpx.MockTransport(handler)
    async with httpx.AsyncClient(transport=transport) as http_client:
        client = OpenAICompatibleLlmClient(
            base_url="https://llm.local/v1",
            api_key="test-key",
            model="test-model",
            client=http_client,
        )

        result = await client.complete_json("system", "user", RecommendationState)

    assert result.summary == "ok"
    assert requests[0].url == "https://llm.local/v1/chat/completions"
    assert requests[0].headers["Authorization"] == "Bearer test-key"
    assert '"response_format":{"type":"json_object"}' in requests[0].content.decode()


async def test_openai_compatible_llm_client_accepts_markdown_json_fence() -> None:
    async def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            json={
                "choices": [
                    {
                        "message": {
                            "content": (
                                "```json\n"
                                '{"summary":"ok","risk_level":"medium","judgment":"needs review",'
                                '"recommended_actions":["check"],"missing_information":[],'
                                '"human_confirmation_required":true}'
                                "\n```"
                            )
                        }
                    }
                ]
            },
        )

    transport = httpx.MockTransport(handler)
    async with httpx.AsyncClient(transport=transport) as http_client:
        client = OpenAICompatibleLlmClient(
            base_url="https://llm.local/v1",
            api_key="test-key",
            model="test-model",
            client=http_client,
        )

        result = await client.complete_json("system", "user", RecommendationState)

    assert result.summary == "ok"


async def test_openai_compatible_llm_client_streams_reasoning_and_content_chunks() -> None:
    async def handler(request: httpx.Request) -> httpx.Response:
        lines = [
            'data: {"choices":[{"delta":{"reasoning_content":"先判断异常对象"}}]}',
            'data: {"choices":[{"delta":{"content":"{\\"action\\":\\"final_answer\\"}"}}]}',
            "data: [DONE]",
        ]
        return httpx.Response(200, content=("\n\n".join(lines) + "\n\n").encode())

    transport = httpx.MockTransport(handler)
    async with httpx.AsyncClient(transport=transport) as http_client:
        client = OpenAICompatibleLlmClient(
            base_url="https://llm.local/v1",
            api_key="test-key",
            model="test-model",
            client=http_client,
        )

        chunks = [
            chunk
            async for chunk in client.stream_chat(
                system_prompt="system",
                user_prompt="user",
                response_format={"type": "json_object"},
            )
        ]

    assert [(chunk.channel, chunk.delta) for chunk in chunks] == [
        ("reasoning_content", "先判断异常对象"),
        ("content", '{"action":"final_answer"}'),
    ]
