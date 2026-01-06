import asyncio
import uuid

from temporalio.client import Client
from parent_workflow import ProcessOrderWorkflow

async def main() -> None:
    client = await Client.connect("localhost:7233")

    from models import Order, OrderItem

    # Order 1: Small simple order
    order1 = Order(
        order_id="ORD-2025-001",
        customer_name="Sarah Chen",
        customer_email="sarah.chen@email.com",
        items=[
            OrderItem(product_id="BOOK-123", quantity=1, price_per_unit=29.99),
            OrderItem(product_id="MUG-456", quantity=2, price_per_unit=12.50),
        ],
        total_amount=54.99,
        shipping_address="123 Maple St, Portland, OR 97201"
    )

    # Order 2: Medium order
    order2 = Order(
        order_id="ORD-2025-002",
        customer_name="James Rodriguez",
        customer_email="james.r@techcorp.com",
        items=[
            OrderItem(product_id="LAPTOP-789", quantity=1, price_per_unit=45.00),
            OrderItem(product_id="MOUSE-321", quantity=1, price_per_unit=25.99),
            OrderItem(product_id="KEYB-654", quantity=1, price_per_unit=89.99),
        ],
        total_amount=160.98,
        shipping_address="456 Tech Blvd, Austin, TX 78701"
    )

    # Order 3: Large order
    order3 = Order(
        order_id="ORD-2025-003",
        customer_name="Emma Thompson",
        customer_email="emma.t@example.org",
        items=[
            OrderItem(product_id="DESK-001", quantity=1, price_per_unit=299.00),
            OrderItem(product_id="CHAIR-002", quantity=1, price_per_unit=399.00),
            OrderItem(product_id="LAMP-003", quantity=2, price_per_unit=35.00),
            OrderItem(product_id="PLANT-004", quantity=3, price_per_unit=15.00),
        ],
        total_amount=813.00,
        shipping_address="789 Oak Ave, Seattle, WA 98101"
    )

    # Order 4: Budget order
    order4 = Order(
        order_id="ORD-2025-004",
        customer_name="Michael Park",
        customer_email="m.park@mail.com",
        items=[
            OrderItem(product_id="PEN-111", quantity=1, price_per_unit=8.99),
            OrderItem(product_id="NOTE-222", quantity=2, price_per_unit=5.50),
        ],
        total_amount=19.99,
        shipping_address="321 Pine Dr, Denver, CO 80201"
    )

    # Order 5: International order
    order5 = Order(
        order_id="ORD-2025-005",
        customer_name="Yuki Tanaka",
        customer_email="yuki.tanaka@example.jp",
        items=[
            OrderItem(product_id="BOOK-999", quantity=1, price_per_unit=42.00),
            OrderItem(product_id="DICT-888", quantity=1, price_per_unit=125.00),
        ],
        total_amount=167.00,
        shipping_address="Tokyo, Shibuya-ku, Japan 150-0001"
    )

    orders = [order1, order2, order3, order4, order5]

    for order in orders:
        await client.start_workflow(
            ProcessOrderWorkflow.run,
            args=[order],
            id=f"order-workflow-{order.order_id}-{uuid.uuid4().hex[:8]}",
            task_queue="order-workflows",
        )
        print(f"Started workflow for {order.order_id}")

if __name__ == "__main__":
    asyncio.run(main())