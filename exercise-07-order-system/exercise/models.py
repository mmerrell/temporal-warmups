# models.py - Data models for order fulfillment system
from dataclasses import dataclass
from typing import List, Optional
from datetime import datetime

@dataclass
class Product:
    """Product in the catalog"""
    product_id: str
    name: str
    price: float
    weight_kg: float  # For shipping calculation

@dataclass
class OrderItem:
    """Single item in an order"""
    product_id: str
    quantity: int
    price_per_unit: float

@dataclass
class Order:
    """Customer order"""
    order_id: str
    customer_name: str
    customer_email: str
    items: List[OrderItem]
    total_amount: float
    shipping_address: str

@dataclass
class Payment:
    """Payment record"""
    payment_id: str
    order_id: str
    amount: float
    payment_method: str
    status: str  # 'pending', 'completed', 'failed', 'refunded'
    processed_at: Optional[str] = None

@dataclass
class InventoryItem:
    """Inventory tracking"""
    product_id: str
    location: str  # Warehouse location
    quantity_available: int
    quantity_reserved: int

@dataclass
class InventoryReservation:
    """Reserved inventory for an order"""
    reservation_id: str
    order_id: str
    product_id: str
    quantity: int
    warehouse_location: str
    reserved_at: str
    status: str  # 'reserved', 'fulfilled', 'cancelled'

@dataclass
class Shipment:
    """Shipping arrangement"""
    shipment_id: str
    order_id: str
    carrier: str
    tracking_number: str
    estimated_delivery: str
    status: str  # 'pending', 'shipped', 'delivered', 'cancelled'
    created_at: str

@dataclass
class Notification:
    """Customer notification record"""
    notification_id: str
    order_id: str
    notification_type: str  # 'order_confirmation', 'shipping_notification', 'cancellation'
    sent_to: str
    sent_at: str
    status: str  # 'sent', 'failed'