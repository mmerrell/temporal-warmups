package exercise;

import exercise.domain.Order;
import exercise.domain.Order.OrderItem;

import java.util.*;

/**
 * Pre-Temporal monolithic order lifecycle service.
 *
 * This code processes orders from creation to delivery, but has several problems
 * that make it fragile, hard to debug, and impossible to scale reliably.
 *
 * YOUR MISSION: Analyze this code, identify the problems, then rebuild it
 * using Temporal workflows and activities.
 */
public class OrderLifecycleService {

    // ---- In-memory state (lost on crash!) ----
    private final Map<String, String> orderStatuses = new HashMap<>();   // orderId → status
    private final Map<String, String> payments = new HashMap<>();        // orderId → paymentId
    private final Map<String, Integer> inventory = new HashMap<>();      // sku → quantity
    private final Map<String, String> shipments = new HashMap<>();       // orderId → trackingNumber
    private final Map<String, String> reservations = new HashMap<>();    // orderId → reservationId

    public OrderLifecycleService() {
        // Seed some inventory
        inventory.put("LAPTOP-001", 50);
        inventory.put("MOUSE-002", 200);
        inventory.put("KEYBOARD-003", 150);
        inventory.put("MONITOR-004", 30);
        inventory.put("HEADSET-005", 75);
    }

    // =========================================================================
    // MAIN PROCESSING METHOD - This is the monolith you'll break apart
    // =========================================================================

    public String processOrder(Order order) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Processing order: " + order.orderId);
        System.out.println("=".repeat(60));

        updateStatus(order.orderId, "CREATED");

        // ---- Step 1: Validate Order ----
        System.out.println("\n[Step 1] Validating order...");
        try {
            validateOrder(order);
            updateStatus(order.orderId, "VALIDATED");
            System.out.println("  ✅ Order validated successfully");
        } catch (Exception e) {
            updateStatus(order.orderId, "VALIDATION_FAILED");
            System.out.println("  ❌ Validation failed: " + e.getMessage());
            return "FAILED: " + e.getMessage();
        }

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

    // =========================================================================
    // "SERVICE" METHODS - These simulate external service calls
    // =========================================================================

    private void validateOrder(Order order) {
        if (order.items == null || order.items.isEmpty()) {
            throw new RuntimeException("Order has no items");
        }
        for (OrderItem item : order.items) {
            if (item.quantity <= 0) {
                throw new RuntimeException("Invalid quantity for item: " + item.sku);
            }
            if (item.price <= 0) {
                throw new RuntimeException("Invalid price for item: " + item.sku);
            }
        }
    }

    private String processPayment(String orderId, String email, double amount) {
        // Simulated 15% failure rate
        if (Math.random() < 0.15) {
            throw new RuntimeException("Payment gateway timeout");
        }
        // BUG: Using System.currentTimeMillis() is non-deterministic
        String paymentId = "PAY-" + System.currentTimeMillis();
        System.out.println("    → Charged $" + String.format("%.2f", amount) + " to " + email);
        return paymentId;
    }

    private String reserveInventory(String orderId, List<OrderItem> items) {
        // Simulated 20% failure rate
        if (Math.random() < 0.20) {
            throw new RuntimeException("Inventory service unavailable");
        }

        for (OrderItem item : items) {
            int available = inventory.getOrDefault(item.sku, 0);
            if (available < item.quantity) {
                throw new RuntimeException("Insufficient stock for " + item.sku
                        + " (need " + item.quantity + ", have " + available + ")");
            }
        }

        // Deduct inventory
        for (OrderItem item : items) {
            inventory.merge(item.sku, -item.quantity, Integer::sum);
        }

        String reservationId = "RES-" + System.currentTimeMillis();
        return reservationId;
    }

    private String createShipment(String orderId, String address) {
        // Simulated 10% failure rate
        if (Math.random() < 0.10) {
            throw new RuntimeException("Shipping service unavailable");
        }
        String trackingNumber = "TRK-" + System.currentTimeMillis();
        System.out.println("    → Shipping to: " + address);
        return trackingNumber;
    }

    private String checkDeliveryStatus(String trackingNumber) {
        // BUG: Math.random() is non-deterministic - breaks Temporal replay!
        double rand = Math.random();
        if (rand < 0.3) return "in_transit";
        if (rand < 0.6) return "out_for_delivery";
        return "delivered";
    }

    private void sendNotification(String email, String orderId, String status, String message) {
        // 5% failure rate (non-critical)
        if (Math.random() < 0.05) {
            throw new RuntimeException("Email service timeout");
        }
        System.out.println("    → Email to " + email + ": " + message);
    }

    // =========================================================================
    // STATUS TRACKING - Only works if you have access to this JVM!
    // =========================================================================

    private void updateStatus(String orderId, String status) {
        orderStatuses.put(orderId, status);
        // BUG: Using System.out.println in what should be workflow logic
        System.out.println("  [STATUS] " + orderId + " → " + status);
    }

    /**
     * Get order status - but this only works if you have access to THIS running JVM.
     * If the process crashes, all status information is lost.
     * If another service needs the status, it can't get it.
     *
     * THIS IS WHY YOU NEED TEMPORAL QUERIES!
     * With @QueryMethod, any external client can read workflow state at any time,
     * even if the workflow is mid-execution or sleeping during delivery tracking.
     */
    public String getOrderStatus(String orderId) {
        return orderStatuses.getOrDefault(orderId, "UNKNOWN");
    }

    public String getTrackingNumber(String orderId) {
        return shipments.getOrDefault(orderId, "NOT_SHIPPED");
    }

    // =========================================================================
    // MAIN - Process sample orders
    // =========================================================================

    public static void main(String[] args) {
        OrderLifecycleService service = new OrderLifecycleService();

        // Create sample orders
        List<Order> orders = List.of(
                new Order("ORD-001", "alice@example.com", "123 Main St, Springfield, IL 62701",
                        List.of(
                                new OrderItem("LAPTOP-001", "Pro Laptop 15\"", 1, 1299.99),
                                new OrderItem("MOUSE-002", "Wireless Mouse", 2, 29.99)
                        )),
                new Order("ORD-002", "bob@example.com", "456 Oak Ave, Portland, OR 97201",
                        List.of(
                                new OrderItem("MONITOR-004", "27\" 4K Monitor", 2, 499.99),
                                new OrderItem("KEYBOARD-003", "Mechanical Keyboard", 1, 149.99)
                        )),
                new Order("ORD-003", "carol@example.com", "789 Pine Rd, Austin, TX 78701",
                        List.of(
                                new OrderItem("HEADSET-005", "Noise-Canceling Headset", 3, 199.99)
                        ))
        );

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     ORDER LIFECYCLE SERVICE (Pre-Temporal Version)           ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Watch for these problems:                                   ║");
        System.out.println("║  • Thread.sleep() blocking during delivery tracking          ║");
        System.out.println("║  • Math.random() and System.currentTimeMillis() usage        ║");
        System.out.println("║  • No way to query order status from outside this JVM        ║");
        System.out.println("║  • No compensation when payment/inventory is orphaned        ║");
        System.out.println("║  • Manual retry logic with hardcoded attempts                ║");
        System.out.println("║  • System.out.println instead of replay-safe logging         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        Map<String, String> results = new LinkedHashMap<>();

        for (Order order : orders) {
            String result = service.processOrder(order);
            results.put(order.orderId, result);
        }

        // Summary
        System.out.println("\n\n" + "═".repeat(60));
        System.out.println("SUMMARY");
        System.out.println("═".repeat(60));
        for (Map.Entry<String, String> entry : results.entrySet()) {
            String icon = entry.getValue().equals("SUCCESS") ? "✅" : "❌";
            System.out.println(icon + " " + entry.getKey() + ": " + entry.getValue());
            System.out.println("   Final status: " + service.getOrderStatus(entry.getKey()));
            System.out.println("   Tracking:     " + service.getTrackingNumber(entry.getKey()));
        }

        System.out.println("\n⚠️  PROBLEMS WITH THIS APPROACH:");
        System.out.println("  1. If this process crashes mid-order, ALL state is lost");
        System.out.println("  2. Delivery tracking blocks the thread for seconds (days in real life!)");
        System.out.println("  3. No external visibility - can't query order status from another service");
        System.out.println("  4. Failed inventory/shipping leaves payment orphaned (no refund!)");
        System.out.println("  5. Math.random() and System.currentTimeMillis() are non-deterministic");
        System.out.println("  6. Manual retry loop instead of configurable retry policies");
        System.out.println("\n💡 YOUR TASK: Rebuild this with Temporal workflows and activities!");
        System.out.println("   New concept: Use @QueryMethod to expose order status to any client!");
    }
}
