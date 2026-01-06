# order_system.py - Monolithic order processing (WITHOUT child workflows)
# This demonstrates the problem that parent-child workflows solve

import random
import time
import uuid
from datetime import datetime, timedelta
from typing import Dict, List
from models import (
    Order, OrderItem, Product, Payment, InventoryItem,
    InventoryReservation, Shipment, Notification
)

# "Database" - in-memory storage
products: Dict[str, Product] = {}
inventory: Dict[str, List[InventoryItem]] = {}
payments: Dict[str, Payment] = {}
reservations: Dict[str, List[InventoryReservation]] = {}
shipments: Dict[str, Shipment] = {}
notifications: Dict[str, Notification] = {}


def process_payment(order: Order) -> Payment:
    """
    Process payment for the order
    Simulates failures 15% of the time
    """
    print(f"\n💳 Processing payment for order {order.order_id}...")
    print(f"   Amount: ${order.total_amount:.2f}")

    time.sleep(0.3)

    # Simulate payment failures
    if random.random() < 0.15:
        raise Exception("Payment processing failed - card declined")

    payment_id = f"PAY-{uuid.uuid4().hex[:8]}"
    payment = Payment(
        payment_id=payment_id,
        order_id=order.order_id,
        amount=order.total_amount,
        payment_method="credit_card",
        status="completed",
        processed_at=datetime.now().isoformat()
    )

    payments[payment_id] = payment
    print(f"   ✅ Payment processed: {payment_id}")
    return payment


def reserve_inventory(order: Order) -> List[InventoryReservation]:
    """
    Reserve inventory for all items in the order
    Simulates failures 20% of the time
    """
    print(f"\n📦 Reserving inventory for order {order.order_id}...")

    time.sleep(0.3)

    # Simulate inventory service failures
    if random.random() < 0.20:
        raise Exception("Inventory service unavailable")

    order_reservations = []

    for item in order.items:
        # Simulate out of stock
        if random.random() < 0.10:
            raise Exception(f"Insufficient inventory for product {item.product_id}")

        reservation_id = f"RES-{uuid.uuid4().hex[:8]}"
        reservation = InventoryReservation(
            reservation_id=reservation_id,
            order_id=order.order_id,
            product_id=item.product_id,
            quantity=item.quantity,
            warehouse_location="WAREHOUSE-A",
            reserved_at=datetime.now().isoformat(),
            status="reserved"
        )
        order_reservations.append(reservation)

        if order.order_id not in reservations:
            reservations[order.order_id] = []
        reservations[order.order_id].append(reservation)

    print(f"   ✅ Reserved {len(order_reservations)} items")
    return order_reservations


def arrange_shipping(order: Order) -> Shipment:
    """
    Arrange shipping for the order
    Simulates failures 10% of the time
    """
    print(f"\n🚚 Arranging shipping for order {order.order_id}...")

    time.sleep(0.3)

    # Simulate shipping service failures
    if random.random() < 0.10:
        raise Exception("Shipping service unavailable")

    shipment_id = f"SHIP-{uuid.uuid4().hex[:8]}"
    tracking = f"1Z{random.randint(100000, 999999)}"

    shipment = Shipment(
        shipment_id=shipment_id,
        order_id=order.order_id,
        carrier="UPS",
        tracking_number=tracking,
        estimated_delivery=(datetime.now() + timedelta(days=3)).strftime('%Y-%m-%d'),
        status="pending",
        created_at=datetime.now().isoformat()
    )

    shipments[shipment_id] = shipment
    print(f"   ✅ Shipping arranged: {tracking}")
    return shipment


def send_notification(order: Order, notification_type: str) -> Notification:
    """
    Send customer notification
    Can fail, but we don't care - order is already processed
    """
    print(f"\n📧 Sending {notification_type} to {order.customer_email}...")

    time.sleep(0.2)

    # Email can fail 5% of the time - but this is OK!
    if random.random() < 0.05:
        print(f"   ⚠️  Email failed (but order is still valid)")
        return None

    notification_id = f"NOTIF-{uuid.uuid4().hex[:8]}"
    notification = Notification(
        notification_id=notification_id,
        order_id=order.order_id,
        notification_type=notification_type,
        sent_to=order.customer_email,
        sent_at=datetime.now().isoformat(),
        status="sent"
    )

    notifications[notification_id] = notification
    print(f"   ✅ Notification sent")
    return notification


# ========================================
# THE PROBLEM: Everything in one function!
# ========================================

def process_order(order: Order) -> str:
    """
    Process a complete order: payment → inventory → shipping → notification

    THE PROBLEM:
    - Everything in one giant function
    - Hard to monitor individual steps
    - Can't retry just one step
    - All state mixed together
    - No independent visibility into sub-processes

    This is what parent-child workflows solve!
    """
    print("\n" + "=" * 70)
    print(f"PROCESSING ORDER: {order.order_id}")
    print(f"Customer: {order.customer_name}")
    print(f"Items: {len(order.items)}")
    print(f"Total: ${order.total_amount:.2f}")
    print("=" * 70)

    payment = None
    inventory = None
    shipping = None

    try:
        # Step 1: Process Payment
        payment = process_payment(order)

        # Step 2: Reserve Inventory
        inventory = reserve_inventory(order)

        # Step 3: Arrange Shipping
        shipping = arrange_shipping(order)

        # Step 4: Send Notification (can fail, don't care)
        send_notification(order, "order_confirmation")

        # Success!
        print("\n" + "=" * 70)
        print(f"✅ ORDER {order.order_id} COMPLETED SUCCESSFULLY!")
        print(f"Payment: {payment.payment_id}")
        print(f"Tracking: {shipping.tracking_number}")
        print("=" * 70 + "\n")

        return order.order_id

    except Exception as e:
        # ========================================
        # CRITICAL PROBLEM: NO COMPENSATIONS!
        # ========================================
        print("\n" + "=" * 70)
        print(f"❌ ORDER {order.order_id} FAILED: {e}")
        print("=" * 70)

        # We should compensate, but we don't!
        if payment:
            print(f"⚠️  WARNING: Payment {payment.payment_id} was processed but not refunded!")
            print("   Customer will be charged for a failed order!")

        if inventory:
            print(f"⚠️  WARNING: Inventory was reserved but not released!")
            print("   Products stuck in 'reserved' state!")

        if shipping:
            print(f"⚠️  WARNING: Shipping was arranged!")

        print("\n💡 Parent-child workflows would give us:")
        print("   - Independent monitoring of each step")
        print("   - Ability to retry individual steps")
        print("   - Separate histories for debugging")
        print("   - Better compensation handling")
        print("=" * 70 + "\n")

        raise


def print_system_state():
    """Show current state of all orders"""
    print("\n" + "=" * 70)
    print("SYSTEM STATE")
    print("=" * 70)
    print(f"Payments: {len(payments)}")
    print(f"Reservations: {sum(len(r) for r in reservations.values())}")
    print(f"Shipments: {len(shipments)}")
    print(f"Notifications: {len(notifications)}")
    print("\n⚠️  If numbers don't match, we have orphaned records!")
    print("=" * 70 + "\n")


# ========================================
# DEMO: Run this to see the problem!
# ========================================

if __name__ == "__main__":
    # Initialize some products
    products = {
        "PROD-001": Product("PROD-001", "Laptop", 999.99, 2.5),
        "PROD-002": Product("PROD-002", "Mouse", 29.99, 0.2),
        "PROD-003": Product("PROD-003", "Keyboard", 79.99, 0.8),
    }

    # Create test orders
    orders = [
        Order(
            order_id=f"ORDER-{i:03d}",
            customer_name=f"Customer {i}",
            customer_email=f"customer{i}@example.com",
            items=[
                OrderItem("PROD-001", 1, 999.99),
                OrderItem("PROD-002", 2, 29.99),
            ],
            total_amount=1059.97,
            shipping_address=f"{i} Main St"
        )
        for i in range(1, 11)
    ]

    print("🎯 DEMONSTRATING THE PARENT-CHILD WORKFLOW PROBLEM")
    print("(Run this a few times - various steps fail randomly)\n")

    # Process orders
    for order in orders:
        try:
            process_order(order)
        except Exception:
            pass  # Already logged

        time.sleep(0.5)

    # Show the problem
    print_system_state()

    print("\n" + "=" * 70)
    print("THE PROBLEM:")
    print("• Everything in one monolithic function")
    print("• No visibility into individual steps")
    print("• Can't retry just payment or just shipping")
    print("• Orphaned records when failures occur")
    print("\nTHE SOLUTION:")
    print("• Break into parent-child workflows")
    print("• Each step (payment, inventory, shipping) is a child workflow")
    print("• Parent orchestrates the children")
    print("• Each child has independent history and monitoring")
    print("=" * 70 + "\n")