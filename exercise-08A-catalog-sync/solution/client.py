import asyncio
import uuid

from temporalio.client import Client
from workflow import CatalogSyncSystem

async def main():
    client = await Client.connect("localhost:7233")
    result = await client.execute_workflow(
        CatalogSyncSystem.sync_catalog,
        args=[],
        id=f"CATALOG-{uuid.uuid4()}",
        task_queue="catalog-queue"
    )

    print(f"\nFinal result: {result}")

if __name__ == "__main__":
    asyncio.run(main())