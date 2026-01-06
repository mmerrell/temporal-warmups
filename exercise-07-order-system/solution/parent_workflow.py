from datetime import timedelta
from temporalio import workflow
from temporalio.common import RetryPolicy
from models import Order
from child_workflows import (
    ProcessPaymentWorkflow,
    ReserveInventoryWorkflow,
    ArrangeShippingWorkflow,
    SendNotificationWorkflow
)

with workflow.unsafe.imports_passed_through():
    from activities import cancel_inventory_activity, cancel_payment_activity, cancel_shipping_activity

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

@workflow.defn
class ProcessOrderWorkflow:

    def __init__(self):
        self.payment_id = None
        self.order_reservations = []
        self.shipping_id = None

    @workflow.run
    async def run(self, order: Order) -> str:
        workflow.logger.info("=" * 70)
        workflow.logger.info(f"PROCESSING ORDER: {order.order_id}")
        workflow.logger.info(f"Customer: {order.customer_name}")
        workflow.logger.info(f"Items: {len(order.items)}")
        workflow.logger.info(f"Total: ${order.total_amount:.2f}")
        workflow.logger.info("=" * 70)

        try:
            # Step 1: Process Payment
            self.payment_id = await workflow.execute_child_workflow(
                ProcessPaymentWorkflow.process_payment,
                args=[order],
                task_queue="order-workflows",
                id=f"payment-{order.order_id}",
            )
            workflow.logger.info(f"✓ Payment processed: {self.payment_id}")

            # Step 2: Reserve Inventory
            self.order_reservations = await workflow.execute_child_workflow(
                ReserveInventoryWorkflow.reserve_inventory,
                args=[order],
                task_queue="order-workflows",
                id=f"inventory-{order.order_id}",
            )
            workflow.logger.info(f"✓ Inventory reserved: {len(self.order_reservations)} items")

            # Step 3: Arrange Shipping
            self.shipping_id = await workflow.execute_child_workflow(
                ArrangeShippingWorkflow.arrange_shipping,
                args=[order],
                task_queue="order-workflows",
                id=f"shipping-{order.order_id}",
            )
            workflow.logger.info(f"✓ Shipping arranged: {self.shipping_id}")

            # Step 4: Send Notification (best effort)
            try:
                await workflow.execute_child_workflow(
                    SendNotificationWorkflow.send_notification,
                    args=[order, "order_confirmation"],
                    task_queue="order-workflows",
                    id=f"notification-{order.order_id}",
                )
                workflow.logger.info("✓ Notification sent")
            except Exception as e:
                workflow.logger.warning(f"⚠️  Notification failed (non-critical): {e}")

            # Success!
            workflow.logger.info("=" * 70)
            workflow.logger.info(f"✅ ORDER {order.order_id} COMPLETED SUCCESSFULLY!")
            workflow.logger.info("=" * 70)

            return f"Order {order.order_id} completed"

        except Exception as e:
            workflow.logger.error("=" * 70)
            workflow.logger.error(f"❌ ORDER {order.order_id} FAILED: {e}")
            workflow.logger.error("=" * 70)

            # Compensate in reverse order
            if self.shipping_id:
                workflow.logger.info(f"↩️  Cancelling shipping: {self.shipping_id}")
                await workflow.execute_activity(
                    cancel_shipping_activity,
                    self.shipping_id,
                    start_to_close_timeout=timedelta(seconds=10),
                    retry_policy=DEFAULT_RETRY_POLICY
                )

            if self.order_reservations:
                workflow.logger.info(f"↩️  Cancelling {len(self.order_reservations)} reservations")
                for reservation_id in self.order_reservations:
                    await workflow.execute_activity(
                        cancel_inventory_activity,
                        reservation_id,
                        start_to_close_timeout=timedelta(seconds=10),
                        retry_policy=DEFAULT_RETRY_POLICY
                    )

            if self.payment_id:
                workflow.logger.info(f"↩️  Refunding payment: {self.payment_id}")
                await workflow.execute_activity(
                    cancel_payment_activity,
                    self.payment_id,
                    start_to_close_timeout=timedelta(seconds=10),
                    retry_policy=DEFAULT_RETRY_POLICY
                )

            raise