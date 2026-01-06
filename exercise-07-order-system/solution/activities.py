import asyncio
import random
import uuid
from temporalio import activity
from temporalio.exceptions import ApplicationError

@activity.defn
async def process_payment_activity(order_id: str, amount: float) -> str:
    """Process payment and return payment_id"""
    print(f"\n💳 Processing payment for order {order_id}...")
    print(f"   Amount: ${amount:.2f}")

    await asyncio.sleep(0.3)

    # Simulate payment failures
    if random.random() < 0.15:
        raise ApplicationError("Payment processing failed - card declined")

    # Generate payment ID
    payment_id = f"PAY-{uuid.uuid4().hex[:8]}"
    print(f"   ✅ Payment processed: {payment_id}")

    return payment_id

@activity.defn
async def reserve_inventory_activity(order_id: str, product_id: str, quantity: int) -> str:
    """Reserve inventory for one item and return reservation_id"""
    print(f"\n📦 Reserving {quantity}x {product_id} for order {order_id}...")

    await asyncio.sleep(0.3)

    # Simulate inventory service failures
    if random.random() < 0.20:
        raise ApplicationError("Inventory service unavailable")

    # Generate reservation ID
    reservation_id = f"RES-{uuid.uuid4().hex[:8]}"
    print(f"   ✅ Reserved: {reservation_id}")

    return reservation_id

@activity.defn
async def arrange_shipping_activity(order_id: str, shipping_address: str) -> str:
    """Arrange shipping and return shipment_id"""
    print(f"\n🚚 Arranging shipping for order {order_id}...")
    print(f"   To: {shipping_address}")

    await asyncio.sleep(0.3)

    # Simulate shipping service failures
    if random.random() < 0.10:
        raise ApplicationError("Shipping service unavailable")

    # Generate shipment ID and tracking
    shipment_id = f"SHIP-{uuid.uuid4().hex[:8]}"
    tracking = f"1Z{random.randint(100000, 999999)}"

    print(f"   ✅ Shipping arranged: {shipment_id}")
    print(f"   📍 Tracking: {tracking}")

    return shipment_id

@activity.defn
async def send_notification_activity(customer_email: str, notification_type: str, order_id: str):
    """Send notification email"""
    print(f"\n📧 Sending {notification_type} to {customer_email}...")
    print(f"   Order: {order_id}")

    await asyncio.sleep(0.2)

    # Email can fail 5% of the time - but this is OK!
    if random.random() < 0.05:
        raise ApplicationError("Email service temporarily unavailable")

    print(f"   ✅ Notification sent")

@activity.defn
async def cancel_payment_activity(payment_id: str):
    """Refund a payment"""
    print(f"\n💳 ↩️  REFUNDING payment {payment_id}...")
    await asyncio.sleep(0.3)
    print(f"   ✅ Payment refunded")

@activity.defn
async def cancel_inventory_activity(reservation_id: str):
    """Release an inventory reservation"""
    print(f"\n📦 ↩️  RELEASING reservation {reservation_id}...")
    await asyncio.sleep(0.3)
    print(f"   ✅ Inventory released")

@activity.defn
async def cancel_shipping_activity(shipment_id: str):
    """Cancel a shipment"""
    print(f"\n🚚 ↩️  CANCELLING shipment {shipment_id}...")
    await asyncio.sleep(0.3)
    print(f"   ✅ Shipment cancelled")