import asyncio
import sys

from temporalio.worker import Worker
from temporalio.client import Client
from workflow import CatalogSyncSystem
from activities import calculate_prices, fetch_products, generate_thumbnail

async def main():
    client = await Client.connect("localhost:7233")
    try:
        worker = Worker(
            client,
            task_queue="catalog-queue",
            workflows=[CatalogSyncSystem],
            activities=[calculate_prices, generate_thumbnail, fetch_products],
        )

    except Exception as err:
        print(f"❌ ERROR: {err}", file=sys.stderr, flush=True)
        raise

    await worker.run()

if __name__ == "__main__":
    asyncio.run(main())