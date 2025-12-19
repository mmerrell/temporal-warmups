# order_processing.py
import time
import random
from datetime import datetime
from enum import Enum

class OrderStatus(Enum):
    PENDING = "pending"
    PAYMENT_PROCESSING = "payment_processing"
    PAYMENT_FAILED = "payment_failed"
    INVENTORY_RESERVED = "inventory_reserved"
    INVENTORY_FAILED = "inventory_failed"
    SHIPPING_SCHEDULED = "shipping_scheduled"
    COMPLETED = "completed"
    FAILED = "failed"

class OrderProcessor:
    def __init__(self):
        self.orders = {}
        self.inventory = {
            "SKU001": 50,
            "SKU002": 30,
            "SKU003": 100,
        }
        self.payments_processed = []
        self.shipments_scheduled = []
        
    def validate_order(self, order_id, items, customer_email):
        """Validate order has required fields"""
        print(f"Validating order {order_id}...")
        time.sleep(0.3)
        
        if not order_id or not customer_email:
            raise ValueError("Order ID and customer email required")
        
        if not items or len(items) == 0:
            raise ValueError("Order must contain at least one item")
        
        for item in items:
            if item['sku'] not in self.inventory:
                raise ValueError(f"Unknown SKU: {item['sku']}")
            if item['quantity'] <= 0:
                raise ValueError("Quantity must be positive")
        
        print(f"✓ Order {order_id} validated")
        return True
    
    def process_payment(self, order_id, customer_email, amount):
        """Process payment with retries"""
        print(f"Processing payment for order {order_id}: ${amount}...")
        
        # Naive retry logic built into the function
        max_attempts = 3
        for attempt in range(max_attempts):
            try:
                time.sleep(1.0)  # Simulate payment gateway call
                
                # Simulate payment gateway failures (20% failure rate)
                if random.random() < 0.2:
                    raise Exception(f"Payment gateway timeout (attempt {attempt + 1})")
                
                payment_id = f"PAY_{order_id}_{int(time.time())}"
                self.payments_processed.append({
                    'payment_id': payment_id,
                    'order_id': order_id,
                    'amount': amount,
                    'timestamp': datetime.now()
                })
                
                print(f"✓ Payment processed: {payment_id}")
                return payment_id
                
            except Exception as e:
                print(f"✗ Payment attempt {attempt + 1} failed: {e}")
                if attempt < max_attempts - 1:
                    print(f"Retrying in {(attempt + 1) * 2} seconds...")
                    time.sleep((attempt + 1) * 2)  # Exponential backoff
                else:
                    raise Exception(f"Payment failed after {max_attempts} attempts")
    
    def reserve_inventory(self, order_id, items):
        """Reserve inventory with retry logic"""
        print(f"Reserving inventory for order {order_id}...")
        
        # Check if we have enough stock
        for item in items:
            sku = item['sku']
            quantity = item['quantity']
            
            if self.inventory.get(sku, 0) < quantity:
                raise Exception(f"Insufficient inventory for {sku}: need {quantity}, have {self.inventory.get(sku, 0)}")
        
        # Simulate database operation with retries
        max_attempts = 3
        for attempt in range(max_attempts):
            try:
                time.sleep(0.8)
                
                # Simulate inventory system failures (15% failure rate)
                if random.random() < 0.15:
                    raise Exception(f"Inventory system unavailable (attempt {attempt + 1})")
                
                # Actually reserve the inventory
                for item in items:
                    self.inventory[item['sku']] -= item['quantity']
                
                reservation_id = f"RES_{order_id}_{int(time.time())}"
                print(f"✓ Inventory reserved: {reservation_id}")
                return reservation_id
                
            except Exception as e:
                print(f"✗ Reservation attempt {attempt + 1} failed: {e}")
                if attempt < max_attempts - 1:
                    print(f"Retrying...")
                    time.sleep(1)
                else:
                    raise Exception(f"Inventory reservation failed after {max_attempts} attempts")
    
    def calculate_shipping(self, items, customer_address):
        """Calculate shipping cost"""
        print(f"Calculating shipping...")
        time.sleep(0.5)
        
        total_weight = sum(item['quantity'] * 2 for item in items)  # Assume 2lbs per item
        base_cost = 5.99
        weight_cost = total_weight * 0.50
        
        shipping_cost = base_cost + weight_cost
        print(f"✓ Shipping calculated: ${shipping_cost:.2f}")
        return shipping_cost
    
    def schedule_shipment(self, order_id, items, customer_address, shipping_cost):
        """Schedule shipment with shipping provider"""
        print(f"Scheduling shipment for order {order_id}...")
        
        max_attempts = 3
        for attempt in range(max_attempts):
            try:
                time.sleep(1.2)
                
                # Simulate shipping provider API failures (10% failure rate)
                if random.random() < 0.1:
                    raise Exception(f"Shipping provider API error (attempt {attempt + 1})")
                
                tracking_number = f"TRACK_{order_id}_{random.randint(10000, 99999)}"
                estimated_delivery = datetime.now().timestamp() + (7 * 24 * 60 * 60)  # 7 days from now
                
                self.shipments_scheduled.append({
                    'tracking_number': tracking_number,
                    'order_id': order_id,
                    'estimated_delivery': estimated_delivery
                })
                
                print(f"✓ Shipment scheduled: {tracking_number}")
                return tracking_number
                
            except Exception as e:
                print(f"✗ Shipment scheduling attempt {attempt + 1} failed: {e}")
                if attempt < max_attempts - 1:
                    time.sleep(2)
                else:
                    raise Exception(f"Shipment scheduling failed after {max_attempts} attempts")
    
    def process_order(self, order_id, items, customer_email, customer_address):
        """Main order processing flow - runs all steps"""
        print(f"\n{'='*70}")
        print(f"Processing order {order_id}")
        print(f"Items: {len(items)} item(s)")
        print(f"Customer: {customer_email}")
        print(f"{'='*70}\n")
        
        try:
            # Step 1: Validate
            self.orders[order_id] = {
                'status': OrderStatus.PENDING,
                'items': items,
                'customer_email': customer_email,
                'customer_address': customer_address,
                'created_at': datetime.now()
            }
            
            self.validate_order(order_id, items, customer_email)
            
            # Step 2: Calculate total
            total_amount = sum(item['price'] * item['quantity'] for item in items)
            print(f"Order total: ${total_amount:.2f}")
            
            # Step 3: Process payment
            self.orders[order_id]['status'] = OrderStatus.PAYMENT_PROCESSING
            payment_id = self.process_payment(order_id, customer_email, total_amount)
            self.orders[order_id]['payment_id'] = payment_id
            
            # Step 4: Reserve inventory
            reservation_id = self.reserve_inventory(order_id, items)
            self.orders[order_id]['reservation_id'] = reservation_id
            self.orders[order_id]['status'] = OrderStatus.INVENTORY_RESERVED
            
            # Step 5: Calculate shipping
            shipping_cost = self.calculate_shipping(items, customer_address)
            self.orders[order_id]['shipping_cost'] = shipping_cost
            
            # Step 6: Schedule shipment
            tracking_number = self.schedule_shipment(order_id, items, customer_address, shipping_cost)
            self.orders[order_id]['tracking_number'] = tracking_number
            self.orders[order_id]['status'] = OrderStatus.COMPLETED
            
            print(f"\n{'='*70}")
            print(f"✓ Order {order_id} completed successfully!")
            print(f"Payment ID: {payment_id}")
            print(f"Tracking: {tracking_number}")
            print(f"Total: ${total_amount:.2f} + ${shipping_cost:.2f} shipping")
            print(f"{'='*70}\n")
            
            return {
                'success': True,
                'order_id': order_id,
                'payment_id': payment_id,
                'tracking_number': tracking_number,
                'total': total_amount + shipping_cost
            }
            
        except Exception as e:
            print(f"\n{'='*70}")
            print(f"✗ Order {order_id} failed: {str(e)}")
            print(f"{'='*70}\n")
            
            if order_id in self.orders:
                self.orders[order_id]['status'] = OrderStatus.FAILED
                self.orders[order_id]['error'] = str(e)
            
            # TODO: We should rollback payment and inventory here!
            # But this code doesn't handle that...
            
            return {
                'success': False,
                'order_id': order_id,
                'error': str(e)
            }


# Usage example
if __name__ == "__main__":
    processor = OrderProcessor()
    
    # Sample orders
    orders = [
        {
            'order_id': 'ORD-001',
            'items': [
                {'sku': 'SKU001', 'quantity': 2, 'price': 29.99},
                {'sku': 'SKU002', 'quantity': 1, 'price': 49.99},
            ],
            'customer_email': 'alice@example.com',
            'customer_address': '123 Main St, San Francisco, CA'
        },
        {
            'order_id': 'ORD-002',
            'items': [
                {'sku': 'SKU003', 'quantity': 5, 'price': 9.99},
            ],
            'customer_email': 'bob@example.com',
            'customer_address': '456 Oak Ave, Portland, OR'
        },
    ]
    
    for order_data in orders:
        result = processor.process_order(
            order_data['order_id'],
            order_data['items'],
            order_data['customer_email'],
            order_data['customer_address']
        )
        
        time.sleep(2)  # Pause between orders
    
    print("\n\nFinal Status:")
    print(f"Orders processed: {len(processor.orders)}")
    print(f"Payments collected: {len(processor.payments_processed)}")
    print(f"Shipments scheduled: {len(processor.shipments_scheduled)}")
    print(f"\nRemaining inventory:")
    for sku, count in processor.inventory.items():
        print(f"  {sku}: {count} units")
