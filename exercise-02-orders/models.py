from dataclasses import dataclass
from enum import Enum
from typing import List, Optional

@dataclass
class OrderResult:
    success: bool
    order_id: str
    payment_id: str
    tracking_number: str
    total: Optional[float]
    error: Optional[Exception]

@dataclass
class Item:
    sku: str
    quantity: int
    price: float

@dataclass
class Order:
    order_id: str
    items: List[Item]
    customer_email: str
    customer_address: str

@dataclass
class ProcessedPayment:
    payment_id: str
    order_id: str
    amount: float
    timestamp: str
