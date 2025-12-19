import asyncio
import sys

from temporalio.worker import Worker
from temporalio.client import Client
from workflow import OrderProcessingWorkflow
from activities import validate_order,calculate_total_with_shipping, reserve_inventory, schedule_shipment, process_payment

async def main():
    try:
        client = await Client.connect("localhost:7233")
        worker = Worker(
            client,
            workflows=[OrderProcessingWorkflow],
            task_queue="order-tasks",
            activities=[validate_order,calculate_total_with_shipping,reserve_inventory, schedule_shipment, process_payment],
        )

    except Exception as err:
        print(f"❌ ERROR: {err}", file=sys.stderr, flush=True)
        raise

    await worker.run()

if __name__ == "__main__":
    asyncio.run(main())