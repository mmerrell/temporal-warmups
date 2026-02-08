package solution.temporal;

import exercise.domain.Order;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import solution.temporal.domain.OrderTrackingInfo;

import java.time.Duration;

public class OrderLifecycleWorkflowImpl implements OrderLifecycleWorkflow{
    private static final Logger logger = Workflow.getLogger(OrderLifecycleWorkflowImpl.class);

    // 1. configure how activities should behave
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))  // How long can one attempt take?
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5)) // How long before first retry? 2 sec is better for LLM
                    .setBackoffCoefficient(2)   // Multiply wait time by what?
                    .build())
            .build();

    //2. create activity stubs with non-existent @ActivityInterface. Using Workflow
    private final OrderActivities validateOrderActivity = Workflow.newActivityStub(
            OrderActivities.class, ACTIVITY_OPTIONS
    );

    @Override
    public String processOrder(Order order) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Processing order: " + order.orderId);
        System.out.println("=".repeat(60));

        updateStatus(order.orderId, "CREATED");

        // ---- Step 1: Validate Order ----
        logger.info("\n[Step 1] Validating order...");
        validateOrderActivity.validateOrder(order);

        // Before
//        System.out.println("\n[Step 1] Validating order...");
//        try {
//            validateOrderActivity.validateOrder(order);
//            updateStatus(order.orderId, "VALIDATED");
//            System.out.println("  ✅ Order validated successfully");
//        } catch (Exception e) {
//            updateStatus(order.orderId, "VALIDATION_FAILED");
//            System.out.println("  ❌ Validation failed: " + e.getMessage());
//            return "FAILED: " + e.getMessage();
//        }

        // ---- Step 2: Process Payment (with manual retry) ----
        System.out.println("\n[Step 2] Processing payment...");
        String paymentId = null;
        int maxPaymentAttempts = 3;
        for (int attempt = 1; attempt <= maxPaymentAttempts; attempt++) {
            try {
                paymentId = processPayment(order.orderId, order.customerEmail, order.getTotalAmount());
                payments.put(order.orderId, paymentId);
                updateStatus(order.orderId, "PAID");
                System.out.println("  ✅ Payment processed: " + paymentId);
                break;
            } catch (Exception e) {
                System.out.println("  ⚠️  Payment attempt " + attempt + "/" + maxPaymentAttempts + " failed: " + e.getMessage());
                if (attempt == maxPaymentAttempts) {
                    updateStatus(order.orderId, "PAYMENT_FAILED");
                    System.out.println("  ❌ Payment failed after " + maxPaymentAttempts + " attempts");
                    return "FAILED: Payment could not be processed";
                }
                // BUG: Thread.sleep in business logic
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }

        // ---- Step 3: Reserve Inventory ----
        System.out.println("\n[Step 3] Reserving inventory...");
        String reservationId = null;
        try {
            reservationId = reserveInventory(order.orderId, order.items);
            reservations.put(order.orderId, reservationId);
            updateStatus(order.orderId, "FULFILLING");
            System.out.println("  ✅ Inventory reserved: " + reservationId);
        } catch (Exception e) {
            // BUG: Payment was taken but inventory failed - no refund!
            updateStatus(order.orderId, "INVENTORY_FAILED");
            System.out.println("  ❌ Inventory reservation failed: " + e.getMessage());
            System.out.println("  ⚠️  WARNING: Payment " + paymentId + " was charged but order cannot be fulfilled!");
            System.out.println("  ⚠️  Customer needs manual refund!");
            return "FAILED: Inventory unavailable (payment orphaned!)";
        }

        // ---- Step 4: Create Shipment ----
        System.out.println("\n[Step 4] Creating shipment...");
        String trackingNumber = null;
        try {
            trackingNumber = createShipment(order.orderId, order.customerAddress);
            shipments.put(order.orderId, trackingNumber);
            updateStatus(order.orderId, "SHIPPED");
            System.out.println("  ✅ Shipment created: " + trackingNumber);
        } catch (Exception e) {
            // BUG: Payment charged + inventory reserved but shipping failed - no compensation!
            updateStatus(order.orderId, "SHIPPING_FAILED");
            System.out.println("  ❌ Shipment creation failed: " + e.getMessage());
            System.out.println("  ⚠️  WARNING: Payment charged AND inventory reserved but cannot ship!");
            return "FAILED: Shipping unavailable (payment + inventory orphaned!)";
        }

        // ---- Step 5: Track Delivery (BLOCKING!) ----
        System.out.println("\n[Step 5] Tracking delivery...");
        updateStatus(order.orderId, "IN_TRANSIT");
        sendNotification(order.customerEmail, order.orderId, "SHIPPED",
                "Your order has shipped! Tracking: " + trackingNumber);

        // BUG: This blocks the thread for potentially minutes (days in production!)
        int maxChecks = 3 + (int) (Math.random() * 8);  // 3-10 checks (non-deterministic!)
        for (int check = 1; check <= maxChecks; check++) {
            System.out.println("  📦 Delivery check " + check + "/" + maxChecks + "...");
            String deliveryStatus = checkDeliveryStatus(trackingNumber);
            System.out.println("     Status: " + deliveryStatus);

            if ("delivered".equals(deliveryStatus)) {
                break;
            }

            // BUG: Thread.sleep blocks this thread! In production this would be hours/days
            try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        updateStatus(order.orderId, "DELIVERED");

        // ---- Step 6: Send Delivery Notification ----
        System.out.println("\n[Step 6] Sending delivery notification...");
        try {
            sendNotification(order.customerEmail, order.orderId, "DELIVERED",
                    "Your order has been delivered!");
            System.out.println("  ✅ Delivery notification sent");
        } catch (Exception e) {
            // Non-critical failure - order is already delivered
            System.out.println("  ⚠️  Notification failed (non-critical): " + e.getMessage());
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Order " + order.orderId + " completed! Final status: " + getOrderStatus(order.orderId));
        System.out.println("=".repeat(60));

        return "SUCCESS";
    }

    @Override
    public String getOrderStatus() {
        return "";
    }

    @Override
    public OrderTrackingInfo getTrackingInfo() {
        return null;
    }

}
