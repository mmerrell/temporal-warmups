import asyncio
import uuid
from temporalio.client import Client
from workflow import OrderProcessingWorkflow

from models import Order, Item

# Usage example
async def main():
    # Sample orders
    orders = [
        Order(
            order_id = 'ORD-001',
            items = [
                Item(sku = 'SKU001', quantity = 2, price = 29.99),
                Item(sku = 'SKU002', quantity = 1, price = 49.99),
            ],
            customer_email = 'alice@example.com',
            customer_address = '123 Main St, San Francisco, CA'
        ),
        Order(
            order_id = 'ORD-002',
            items = [
                Item(sku = 'SKU003', quantity = 5, price = 9.99),
            ],
            customer_email = 'bob@example.com',
            customer_address = '456 Oak Ave, Portland, OR'
        ),
    ]

    client = await Client.connect("localhost:7233")
    for order in orders:
        handle = await client.start_workflow(
            OrderProcessingWorkflow.run,
            args=[order],
            id=f"order-{order.order_id}-{uuid.uuid4()}",
            task_queue="order-tasks",
        )

        await asyncio.sleep(2)  # Pause between orders

if __name__ == "__main__":
    asyncio.run(main())