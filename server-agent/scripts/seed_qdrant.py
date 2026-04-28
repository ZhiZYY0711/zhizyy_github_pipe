import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.rag.seed import seed_qdrant_collections


async def main() -> None:
    result = await seed_qdrant_collections()
    print(result)


if __name__ == "__main__":
    asyncio.run(main())
