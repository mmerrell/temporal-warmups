import asyncio
from datetime import timedelta

from temporalio import workflow
from temporalio.common import RetryPolicy

with workflow.unsafe.imports_passed_through():
    from activities import fetch_products, calculate_prices, generate_thumbnail
    from models import Product

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

@workflow.defn
class CatalogSyncSystem:

    @workflow.run
    async def sync_catalog(self) -> dict:
        try:
            suppliers = ["AcmeCo", "GlobalTech", "MegaSupply", "PrimeParts", "DirectGoods"]

            supplier_results = await asyncio.gather(
                *[
                    workflow.execute_activity(
                        fetch_products,
                        args=[supplier],
                        start_to_close_timeout=timedelta(seconds=30),
                        retry_policy=DEFAULT_RETRY_POLICY,
                    )
                    for supplier in suppliers
                ]
            )

            all_products = []
            for products in supplier_results:
                all_products.extend(products)

            workflow.logger.info(f"\n✓ Fetched total of {len(all_products)} products")

            engines = ["CompetitorPrice", "DynamicPrice", "MarketPrice"]

            price_map_results = await asyncio.gather(
                *[
                    workflow.execute_activity(
                        calculate_prices,
                        args=[engine, all_products],
                        start_to_close_timeout=timedelta(seconds=30),
                        retry_policy=DEFAULT_RETRY_POLICY,
                    )
                    for engine in engines
                ]
            )

            all_price_maps = []
            for price_maps in price_map_results:
                all_price_maps.append(price_maps)

            final_prices = {}
            for product in all_products:
                prices = [pm.get(product['product_id'], product['price']) for pm in all_price_maps]
                final_prices[product['product_id']] = round(sum(prices) / len(prices), 2)

            workflow.logger.info(f"\n✓ Calculated prices for {len(final_prices)} products")

            thumbnail_results = await asyncio.gather(
                *[
                    workflow.execute_activity(
                        generate_thumbnail,
                        args=[product['product_id']],
                        start_to_close_timeout=timedelta(seconds=30),
                        retry_policy=DEFAULT_RETRY_POLICY,
                    )
                    for product in all_products[:10]
                ]
            )
            thumbnails = {}
            for i, thumbnail_url in enumerate(thumbnail_results):
                thumbnails[all_products[i]['product_id']] = thumbnail_url

            workflow.logger.info(f"\n✓ Generated {len(thumbnails)} thumbnails")

            workflow.logger.info("\n" + "=" * 70)
            workflow.logger.info(f"✅ CATALOG SYNC COMPLETE!")
            workflow.logger.info(f"   Products synced: {len(all_products)}")
            workflow.logger.info(f"   Prices calculated: {len(final_prices)}")
            workflow.logger.info(f"   Thumbnails generated: {len(thumbnails)}")
            workflow.logger.info("=" * 70 + "\n")

            return {
                'success': True,
                'products': len(all_products),
                'prices': len(final_prices),
                'thumbnails': len(thumbnails),
            }

        except Exception as e:
            workflow.logger.info(f"\n❌ SYNC FAILED: {e}")
            return {
                'success': False,
                'error': str(e),
            }