async def retrieve_domain_knowledge(query: str) -> list[dict]:
    return [
        {
            "id": "rag_mock_001",
            "title": "阴保异常巡检建议",
            "summary": "先确认参比电极、采集窗口和近期维修记录。",
            "source": "domain_knowledge",
            "score": 0.78,
            "query": query,
        }
    ]
