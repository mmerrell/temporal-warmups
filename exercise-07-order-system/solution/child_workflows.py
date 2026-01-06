from datetime import timedelta
from temporalio import workflow
from temporalio.common import RetryPolicy
from models import Order

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

with workflow.unsafe.imports_passed_through():
    from activities import (
        process_payment_activity,
        reserve_inventory_activity,
        arrange_shipping_activity,
        send_notification_activity,
    )

@workflow.defn
class ProcessPaymentWorkflow:
    @workflow.run
    async def process_payment(self, order: Order) -> str:
        """
        Process payment for the order
        Returns: payment_id (string)
        """
        payment_id = await workflow.execute_activity(
            process_payment_activity,
            args=[order.order_id, order.total_amount],
            start_to_close_timeout=timedelta(seconds=10),
            retry_policy=DEFAULT_RETRY_POLICY
        )

        return payment_id

@workflow.defn
class ReserveInventoryWorkflow:
    @workflow.run
    async def reserve_inventory(self, order: Order) -> list[str]:
        """
        Reserve inventory for all items in the order
        Returns: list of reservation_ids (strings)
        """
        reservation_ids = []

        for item in order.items:
            reservation_id = await workflow.execute_activity(
                reserve_inventory_activity,
                args=[order.order_id, item.product_id, item.quantity],
                start_to_close_timeout=timedelta(seconds=10),
                retry_policy=DEFAULT_RETRY_POLICY
            )
            reservation_ids.append(reservation_id)

        return reservation_ids

@workflow.defn
class ArrangeShippingWorkflow:
    @workflow.run
    async def arrange_shipping(self, order: Order) -> str:
        """
        Arrange shipping for the order
        Returns: shipment_id (string)
        """
        shipment_id = await workflow.execute_activity(
            arrange_shipping_activity,
            args=[order.order_id, order.shipping_address],
            start_to_close_timeout=timedelta(seconds=10),
            retry_policy=DEFAULT_RETRY_POLICY
        )

        return shipment_id

@workflow.defn
class SendNotificationWorkflow:
    @workflow.run
    async def send_notification(self, order: Order, notification_type: str):
        """
        Send customer notification
        Can fail, but we don't care - order is already processed
        """
        await workflow.execute_activity(
            send_notification_activity,
            args=[order.customer_email, notification_type, order.order_id],
            start_to_close_timeout=timedelta(seconds=10),
            retry_policy=DEFAULT_RETRY_POLICY
        )