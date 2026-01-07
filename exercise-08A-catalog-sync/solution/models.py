from dataclasses import dataclass

@dataclass
class Product:
    product_id: str
    name: str
    price: float
    stock: int
    supplier: str

