import uuid
from typing import Dict

from temporalio.exceptions import ApplicationError

from models import Item

class InventoryDatabase:
    def __init__(self):
        self._inventory: Dict[str, Item] = {
            "SKU001": Item("SKU001", 50, 10.00),
            "SKU002": Item("SKU002", 30, 20.00),
            "SKU003": Item("SKU003", 100, 30.00),
        }

    def check_availability(self, sku: str, quantity: int) -> bool:
        """Check if we have enough stock"""
        item = self._inventory.get(sku)
        if item is None:
            return False
        return item.quantity >= quantity

    def get_quantity(self, sku: str) -> int:
        item = self._inventory.get(sku)
        return item.quantity if item else 0

    def reserve_inventory(self, sku: str, quantity: int) -> str:
        """Actually decrement the inventory"""
        item = self._inventory.get(sku)
        if item and item.quantity >= quantity:
            item.quantity -= quantity
            return str(uuid.uuid4())
        raise ApplicationError("Insufficient stock")

    def get_stock(self, sku: str) -> int:
        """Get current stock level"""
        item = self._inventory.get(sku)
        return item.quantity if item else 0

# Global instance (in real life, this would be a real database)
inventory_db = InventoryDatabase()