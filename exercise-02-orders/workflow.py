from datetime import timedelta

from temporalio import workflow
from temporalio.common import RetryPolicy
from models import Order, OrderResult

with workflow.unsafe.imports_passed_through():
    from activities import (
        validate_order,
        calculate_total_with_shipping,
        process_payment,
        reserve_inventory,
        schedule_shipment
    )

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

@workflow.defn
class OrderProcessingWorkflow:

    @workflow.run
    async def run(self, order: Order):
        """Main order processing flow - runs all steps"""
        workflow.logger.info(f"\n{'=' * 70}")
        workflow.logger.info(f"Processing order {order.order_id}")
        workflow.logger.info(f"Items: {len(order.items)} item(s)")
        workflow.logger.info(f"Customer: {order.customer_email}")
        workflow.logger.info(f"{'=' * 70}\n")

        try:
            # Step 1: Validate
            await workflow.execute_activity(
                validate_order,
                args=[order],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            # Step 2: Calculate total
            total_amount = await workflow.execute_activity(
                calculate_total_with_shipping,
                args=[order],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )
            workflow.logger.info(f"Order total: ${total_amount:.2f}")

            # Step 3: Process payment
            payment_id = await workflow.execute_activity(
                process_payment,
                args=[order, total_amount],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            # Step 4: Reserve inventory
            reservation_id = await workflow.execute_activity(
                reserve_inventory,
                args=[order],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )
            workflow.logger.info(f"\n{'=' * 70}")
            workflow.logger.info(f"Inventory Reservation successful: {reservation_id}")
            workflow.logger.info(f"\n{'=' * 70}\n")

            # Step 6: Schedule shipment
            tracking_number = await workflow.execute_activity(
                schedule_shipment,
                args=[order],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            workflow.logger.info(f"\n{'=' * 70}")
            workflow.logger.info(f"✓ Order {order.order_id} completed successfully!")
            workflow.logger.info(f"Payment ID: {payment_id}")
            workflow.logger.info(f"Tracking: {tracking_number}")
            workflow.logger.info(f"Total: ${total_amount:.2f}")
            workflow.logger.info(f"{'=' * 70}\n")

            return OrderResult(
                success = True,
                order_id = order.order_id,
                payment_id = payment_id,
                tracking_number = tracking_number,
                total = total_amount,
                error = None
            )

        except Exception as e:
            workflow.logger.info(f"\n{'=' * 70}")
            workflow.logger.info(f"✗ Order {order.order_id} failed: {str(e)}")
            workflow.logger.info(f"{'=' * 70}\n")

            # TODO: We should rollback payment and inventory here!
            # But this code doesn't handle that...

            return OrderResult(
                success=False,
                order_id=order.order_id,
                payment_id=None,
                tracking_number=None,
                total=None,
                error=str(e)
            )

