DOMAIN_KNOWLEDGE_COLLECTION = "domain_knowledge"
SIMILAR_CASES_COLLECTION = "case_knowledge"
CASE_MEMORY_COLLECTION = "case_memory"


def build_async_qdrant_client():
    from qdrant_client import AsyncQdrantClient

    from app.core.config import get_settings

    return AsyncQdrantClient(url=get_settings().qdrant_url, check_compatibility=False)
