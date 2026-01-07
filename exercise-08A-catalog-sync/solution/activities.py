import asyncio
import random
from typing import List
from temporalio import activity
from models import Product

@activity.defn
async def fetch_products(supplier_name: str) -> List[Product]:
    """Fetch products from a supplier - takes 2-5 seconds"""
    print(f"📡 Fetching products from {supplier_name}...")

    # Simulate API call time
    delay = random.uniform(2, 5)
    await asyncio.sleep(delay)

    # Simulate occasional API failures
    if random.random() < 0.15:
        raise Exception(f"{supplier_name} API unavailable")

    # Generate some mock products
    num_products = random.randint(5, 15)
    products = []
    for i in range(num_products):
        products.append(Product(
            product_id=f"{supplier_name}-{i + 1:03d}",
            name=f"{supplier_name} Product {i + 1}",
            price=round(random.uniform(10, 500), 2),
            stock=random.randint(0, 100),
            supplier=supplier_name
        ))

    print(f"✓ Got {len(products)} products from {supplier_name} (took {delay:.1f}s)")
    return products

@activity.defn
async def calculate_prices(engine_name: str, products: List[Product]) -> dict:
    """Calculate competitive prices - takes 1-3 seconds"""
    print(f"💰 Running {engine_name} pricing engine...")

    delay = random.uniform(1, 3)
    await asyncio.sleep(delay)

    # Simulate occasional failures
    if random.random() < 0.10:
        raise Exception(f"{engine_name} pricing engine error")

    # Generate price adjustments
    price_map = {}
    for product in products:
        adjustment = random.uniform(0.9, 1.1)  # ±10%
        price_map[product.product_id] = round(product.price * adjustment, 2)

    print(f"✓ {engine_name} calculated {len(price_map)} prices (took {delay:.1f}s)")
    return price_map

@activity.defn
async def generate_thumbnail(product_id: str) -> str:
    """Generate thumbnail for product image - takes 1-2 seconds"""
    print(f"🖼️  Generating thumbnail for {product_id}...")

    delay = random.uniform(1, 2)
    await asyncio.sleep(delay)

    # Simulate occasional failures
    if random.random() < 0.08:
        raise Exception(f"Image processing failed for {product_id}")

    thumbnail_url = f"https://cdn.example.com/thumbs/{product_id}.jpg"
    print(f"✓ Thumbnail ready: {thumbnail_url}")
    return thumbnail_url
