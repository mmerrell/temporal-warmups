"""
Product Catalog Sync System

Syncs product data from multiple suppliers sequentially.
Problems:
- Takes forever (waits for each supplier one at a time)
- No durability if process crashes
- No automatic retries for failed API calls
- Can't see sync progress
"""

import time
import random
from dataclasses import dataclass
from typing import List


@dataclass
class Product:
    product_id: str
    name: str
    price: float
    stock: int
    supplier: str


class SupplierAPI:
    """Simulates calling external supplier APIs"""

    def fetch_products(self, supplier_name: str) -> List[Product]:
        """Fetch products from a supplier - takes 2-5 seconds"""
        print(f"📡 Fetching products from {supplier_name}...")

        # Simulate API call time
        delay = random.uniform(2, 5)
        time.sleep(delay)

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


class PriceEngine:
    """Simulates calling pricing calculation engines"""

    def calculate_prices(self, engine_name: str, products: List[Product]) -> dict:
        """Calculate competitive prices - takes 1-3 seconds"""
        print(f"💰 Running {engine_name} pricing engine...")

        delay = random.uniform(1, 3)
        time.sleep(delay)

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


class ImageProcessor:
    """Simulates image processing (thumbnails, etc)"""

    def generate_thumbnail(self, product_id: str) -> str:
        """Generate thumbnail for product image - takes 1-2 seconds"""
        print(f"🖼️  Generating thumbnail for {product_id}...")

        delay = random.uniform(1, 2)
        time.sleep(delay)

        # Simulate occasional failures
        if random.random() < 0.08:
            raise Exception(f"Image processing failed for {product_id}")

        thumbnail_url = f"https://cdn.example.com/thumbs/{product_id}.jpg"
        print(f"✓ Thumbnail ready: {thumbnail_url}")
        return thumbnail_url


class CatalogSyncSystem:
    """Main system that syncs catalog - SEQUENTIALLY (slow!)"""

    def __init__(self):
        self.supplier_api = SupplierAPI()
        self.price_engine = PriceEngine()
        self.image_processor = ImageProcessor()

    def sync_catalog(self) -> dict:
        """
        Sync entire product catalog
        PROBLEM: Everything happens sequentially - takes FOREVER!
        """
        print("\n" + "=" * 70)
        print("STARTING CATALOG SYNC")
        print("=" * 70 + "\n")

        start_time = time.time()

        try:
            # ========================================
            # PROBLEM 1: Fetch from suppliers ONE AT A TIME
            # These are independent - could run in parallel!
            # ========================================
            print("\n📦 Phase 1: Fetching from suppliers (SEQUENTIAL - SLOW!)")
            print("-" * 70)

            all_products = []
            suppliers = ["AcmeCo", "GlobalTech", "MegaSupply", "PrimeParts", "DirectGoods"]

            for supplier in suppliers:
                products = self.supplier_api.fetch_products(supplier)
                all_products.extend(products)

            print(f"\n✓ Fetched total of {len(all_products)} products")

            # ========================================
            # PROBLEM 2: Run pricing engines ONE AT A TIME
            # These are also independent - could run in parallel!
            # ========================================
            print("\n💵 Phase 2: Running pricing engines (SEQUENTIAL - SLOW!)")
            print("-" * 70)

            engines = ["CompetitorPrice", "DynamicPrice", "MarketPrice"]
            all_price_maps = []

            for engine in engines:
                price_map = self.price_engine.calculate_prices(engine, all_products)
                all_price_maps.append(price_map)

            # Average the prices from all engines
            final_prices = {}
            for product in all_products:
                prices = [pm.get(product.product_id, product.price) for pm in all_price_maps]
                final_prices[product.product_id] = round(sum(prices) / len(prices), 2)

            print(f"\n✓ Calculated prices for {len(final_prices)} products")

            # ========================================
            # PROBLEM 3: Generate thumbnails ONE AT A TIME
            # These could DEFINITELY run in parallel!
            # ========================================
            print("\n🎨 Phase 3: Generating thumbnails (SEQUENTIAL - SLOW!)")
            print("-" * 70)

            thumbnails = {}
            # Just do first 10 products to keep example short
            for product in all_products[:10]:
                thumbnail_url = self.image_processor.generate_thumbnail(product.product_id)
                thumbnails[product.product_id] = thumbnail_url

            print(f"\n✓ Generated {len(thumbnails)} thumbnails")

            # ========================================
            # Final Results
            # ========================================
            elapsed = time.time() - start_time

            print("\n" + "=" * 70)
            print(f"✅ CATALOG SYNC COMPLETE!")
            print(f"   Products synced: {len(all_products)}")
            print(f"   Prices calculated: {len(final_prices)}")
            print(f"   Thumbnails generated: {len(thumbnails)}")
            print(f"   Total time: {elapsed:.1f} seconds (SLOW!)")
            print("=" * 70 + "\n")

            return {
                'success': True,
                'products': len(all_products),
                'prices': len(final_prices),
                'thumbnails': len(thumbnails),
                'elapsed': elapsed
            }

        except Exception as e:
            elapsed = time.time() - start_time
            print(f"\n❌ SYNC FAILED: {e}")
            print(f"   Failed after {elapsed:.1f} seconds")
            return {
                'success': False,
                'error': str(e),
                'elapsed': elapsed
            }


def main():
    """Run the catalog sync"""
    sync_system = CatalogSyncSystem()
    result = sync_system.sync_catalog()

    print(f"\nFinal result: {result}")


if __name__ == "__main__":
    main()