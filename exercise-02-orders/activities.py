import asyncio
import random
import time
from inventory import inventory_db
from temporalio import activity
from models import Order, ProcessedPayment

@activity.defn
async def calculate_total_with_shipping(order: Order) -> float:
    """Calculate shipping cost"""
    print(f"Calculating shipping...")
    await asyncio.sleep(0.5)

    total_weight = sum(item.quantity * 2 for item in order.items)  # Assume 2lbs per item
    base_cost = 5.99
    weight_cost = total_weight * 0.50

    shipping_cost = base_cost + weight_cost
    print(f"✓ Shipping calculated: ${shipping_cost:.2f}")
    items_total = sum(item.price * item.quantity for item in order.items)
    total_amount = items_total + shipping_cost

    return total_amount

@activity.defn
async def validate_order(order: Order):
    """Validate order has required fields"""
    print(f"Validating order {order.order_id}...")
    await asyncio.sleep(0.3)

    if not order.order_id or not order.customer_email:
        raise ValueError("Order ID and customer email required")

    if not order.items or len(order.items) == 0:
        raise ValueError("Order must contain at least one item")

    for item in order.items:
        if not inventory_db.check_availability(item.sku, item.quantity):
            raise ValueError(f"Unknown SKU or item not available: {item.sku}")

    print(f"✓ Order {order.order_id} validated")

@activity.defn
async def process_payment(order: Order, amount: float):
    """Process payment with retries"""
    print(f"Processing payment for order {order.order_id}: ${amount}...")

    # Simulate payment gateway failures (20% failure rate)
    if random.random() < 0.2:
        raise Exception(f"Payment gateway timeout")

    try:
        payment_id = f"PAY_{order.order_id}_{int(time.time())}"
        return ProcessedPayment(
            payment_id = payment_id,
            order_id = order.order_id,
            amount = amount,
            timestamp = str(time.time()),
        )

    except Exception as e:
        print(f"✗ Payment attempt failed: {e}")
        raise e

@activity.defn
async def reserve_inventory(order: Order):
    """Reserve inventory with retry logic"""
    print(f"Reserving inventory for order {order.order_id}...")

    # Check if we have enough stock
    for item in order.items:
        sku = item.sku
        quantity = item.quantity

        if not inventory_db.check_availability(sku, quantity):
            raise Exception(f"Insufficient inventory for {sku}: need {quantity}, have {inventory_db.get(sku, 0)}")

        # Actually reserve the inventory
        inventory_db.reserve_inventory(item.sku, item.quantity)

        reservation_id = f"RES_{order.order_id}_{int(time.time())}"
        print(f"✓ Inventory reserved: {reservation_id}")
        return reservation_id

    return None


@activity.defn
async def schedule_shipment(order: Order) -> str:
    """Schedule shipment with shipping provider"""
    print(f"Scheduling shipment for order {order.order_id}...")

    # Simulate shipping provider API failures (10% failure rate)
    if random.random() < 0.4:
        raise Exception(f"Shipping provider API error")

    tracking_number = f"TRACK_{order.order_id}_{random.randint(10000, 99999)}"
    estimated_delivery = time.time() + (7 * 24 * 60 * 60)  # 7 days from now

    print(f"✓ Shipment scheduled: {tracking_number}. Estimated delivery time: {estimated_delivery}")
    return tracking_number