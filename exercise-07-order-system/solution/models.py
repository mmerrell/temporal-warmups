# models.py - Data models for order fulfillment system
from dataclasses import dataclass
from typing import Optional

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
class InventoryItem:
    """Inventory tracking"""
    product_id: str
    location: str  # Warehouse location
    quantity_available: int
    quantity_reserved: int

@dataclass
class Order:
    """Customer order"""
    order_id: str
    customer_name: str
    customer_email: str
    items: list[OrderItem]
    total_amount: float
    shipping_address: str

@dataclass
class Payment:
    """Payment record"""
    order_id: str
    amount: float
    payment_method: str
    payment_id: Optional[str] = None  # Generated in activity
    status: str = "pending"
    processed_at: Optional[str] = None

@dataclass
class InventoryReservation:
    """Inventory reservation record"""
    order_id: str
    product_id: str
    quantity: int
    warehouse_location: str
    reservation_id: Optional[str] = None  # Generated in activity
    reserved_at: Optional[str] = None
    status: str = "pending"

@dataclass
class Shipment:
    """Shipment record"""
    order_id: str
    carrier: str
    estimated_delivery: str
    shipment_id: Optional[str] = None  # Generated in activity
    tracking_number: Optional[str] = None  # Generated in activity
    status: str = "pending"
    created_at: Optional[str] = None

@dataclass
class Notification:
    """Notification record"""
    order_id: str
    notification_type: str  # "order_confirmation", "shipping_update", etc.
    sent_to: str  # email address
    notification_id: Optional[str] = None  # Generated in activity
    sent_at: Optional[str] = None
    status: str = "pending"