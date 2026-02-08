package solution.temporal;

import exercise.domain.Order;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import solution.temporal.domain.OrderTrackingInfo;

import java.time.Duration;

public class OrderLifecycleWorkflowImpl implements OrderLifecycleWorkflow {
    private static final Logger logger = Workflow.getLogger(OrderLifecycleWorkflowImpl.class);

    // Workflow state — exposed via @QueryMethod
    private String currentStatus = "CREATED";
    private String paymentId;
    private String reservationId;
    private String trackingNumber;

    // 1. Configure how activities should behave
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60)) //maximum time a single Activity attempt is allowed to run
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5))  //After the first failure, Temporal waits 5 seconds before the first retry attempt.
                    .setBackoffCoefficient(2)   //Each subsequent retry delay is multiplied by 2
                    .build())
            .build();

    // 2. Create activity stub
    private final OrderActivities activities = Workflow.newActivityStub(
            OrderActivities.class, ACTIVITY_OPTIONS
    );

    @Override
    public String processOrder(Order order) {
        logger.info("Processing order: {}", order.orderId);
        currentStatus = "CREATED";

        // ---- Step 1: Validate Order ----
        logger.info("[Step 1] Validating order...");
        activities.validateOrder(order);
        currentStatus = "VALIDATED";
        logger.info("Order validated successfully");

        try {
            // ---- Step 2: Process Payment ----
            logger.info("[Step 2] Processing payment...");
            this.paymentId = activities.processPayment(order.orderId, order.customerEmail, order.getTotalAmount());
            currentStatus = "PAID";
            logger.info("Payment processed: {}", paymentId);

            // ---- Step 3: Reserve Inventory ----
            logger.info("[Step 3] Reserving inventory...");
            this.reservationId = activities.reserveInventory(order.orderId, order.items);
            currentStatus = "FULFILLING";
            logger.info("Inventory reserved: {}", reservationId);

            // ---- Step 4: Create Shipment ----
            logger.info("[Step 4] Creating shipment...");
            this.trackingNumber = activities.createShipment(order.orderId, order.customerAddress);
            currentStatus = "SHIPPED";
            logger.info("Shipment created: {}", trackingNumber);

        } catch (ActivityFailure e) {
            logger.error("Order {} failed at status {}: {}", order.orderId, currentStatus, e.getMessage());
            // Compensate in reverse order — only what succeeded
            if (reservationId != null) {
                logger.info("Compensating: releasing inventory {}", reservationId);
                activities.releaseInventory(reservationId);
            }
            if (paymentId != null) {
                logger.info("Compensating: refunding payment {}", paymentId);
                activities.refundPayment(paymentId);
            }
            currentStatus = "FAILED";
            return "FAILED";
        }

        // ---- Step 5: Track Delivery ----
        logger.info("[Step 5] Tracking delivery...");
        currentStatus = "IN_TRANSIT";
        try {
            activities.sendNotification(order.customerEmail, order.orderId, "SHIPPED");
        } catch (ActivityFailure e) {
            logger.warn("Shipped notification failed (non-critical): {}", e.getMessage());
        }

        int maxChecks = 20;
        for (int check = 1; check <= maxChecks; check++) {
            String deliveryStatus = activities.checkDeliveryStatus(trackingNumber);
            logger.info("Delivery check {}/{}: {}", check, maxChecks, deliveryStatus);

            if ("delivered".equals(deliveryStatus)) {
                break;
            }
            currentStatus = "out_for_delivery".equals(deliveryStatus) ? "OUT_FOR_DELIVERY" : "IN_TRANSIT";
            Workflow.sleep(Duration.ofSeconds(10)); // Durable sleep — use minutes in production
        }

        currentStatus = "DELIVERED";

        // ---- Step 6: Send Delivery Notification ----
        logger.info("[Step 6] Sending delivery notification...");
        try {
            activities.sendNotification(order.customerEmail, order.orderId, "DELIVERED");
        } catch (ActivityFailure e) {
            logger.warn("Delivery notification failed (non-critical): {}", e.getMessage());
        }

        logger.info("Order {} completed!", order.orderId);
        return "SUCCESS";
    }

    @Override
    public String getOrderStatus() {
        return currentStatus;
    }

    @Override
    public OrderTrackingInfo getTrackingInfo() {
        return new OrderTrackingInfo(null, currentStatus, trackingNumber, paymentId, reservationId);
    }
}
