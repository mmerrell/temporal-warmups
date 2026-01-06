import asyncio
import sys

from temporalio.client import Client
from temporalio.worker import Worker
from parent_workflow import ProcessOrderWorkflow
from child_workflows import ProcessPaymentWorkflow, ReserveInventoryWorkflow, SendNotificationWorkflow, ArrangeShippingWorkflow
from activities import (
    process_payment_activity,
    reserve_inventory_activity,
    arrange_shipping_activity,
    send_notification_activity,
    cancel_inventory_activity,
    cancel_payment_activity,
    cancel_shipping_activity
)

async def main():
    try:
        client = await Client.connect("localhost:7233")

        worker = Worker(
            client,
            workflows=[ProcessOrderWorkflow, ProcessPaymentWorkflow, ReserveInventoryWorkflow, SendNotificationWorkflow, ArrangeShippingWorkflow],
            task_queue="order-workflows",
            activities=[
                process_payment_activity,
                reserve_inventory_activity,
                arrange_shipping_activity,
                send_notification_activity,
                cancel_inventory_activity,
                cancel_payment_activity,
                cancel_shipping_activity
            ],
        )

        await worker.run()

    except Exception as err:
        print(f"❌ ERROR: {err}", file=sys.stderr, flush=True)
        raise

if __name__ == "__main__":
    asyncio.run(main())