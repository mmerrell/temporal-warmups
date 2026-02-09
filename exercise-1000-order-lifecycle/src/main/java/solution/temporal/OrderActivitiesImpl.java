package solution.temporal;

import exercise.domain.Order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderActivitiesImpl implements OrderActivities {
    private final Map<String, Integer> inventory = new HashMap<>();      // sku → quantity

    public OrderActivitiesImpl() {
        // Seed some inventory
        inventory.put("LAPTOP-001", 50);
        inventory.put("MOUSE-002", 200);
        inventory.put("KEYBOARD-003", 150);
        inventory.put("MONITOR-004", 30);
        inventory.put("HEADSET-005", 75);
    }

    @Override
    public void validateOrder(Order order) {
        if (order.items == null || order.items.isEmpty()) {
            throw new RuntimeException("Order has no items");
        }
        for (Order.OrderItem item : order.items) {
            if (item.quantity <= 0) {
                throw new RuntimeException("Invalid quantity for item: " + item.sku);
            }
            if (item.price <= 0) {
                throw new RuntimeException("Invalid price for item: " + item.sku);
            }
        }
    }

    @Override
    public String processPayment(String orderId, String email, double amount) {
        // Simulated 15% failure rate
        if (Math.random() < 0.15) {
            throw new RuntimeException("Payment gateway timeout");
        }
        // BUG: Using System.currentTimeMillis() is non-deterministic
        String paymentId = "PAY-" + System.currentTimeMillis();
        System.out.println("    → Charged $" + String.format("%.2f", amount) + " to " + email);
        return paymentId;
    }

    @Override
    public String reserveInventory(String orderId, List<Order.OrderItem> items) {
        // Simulated 20% failure rate
        if (Math.random() < 0.20) {
            throw new RuntimeException("Inventory service unavailable");
        }

        for (Order.OrderItem item : items) {
            int available = inventory.getOrDefault(item.sku, 0);
            if (available < item.quantity) {
                throw new RuntimeException("Insufficient stock for " + item.sku
                        + " (need " + item.quantity + ", have " + available + ")");
            }
        }

        // Deduct inventory
        for (Order.OrderItem item : items) {
            inventory.merge(item.sku, -item.quantity, Integer::sum);
        }

        String reservationId = "RES-" + System.currentTimeMillis();
        return reservationId;
    }

    @Override
    public String createShipment(String orderId, String address) {
        // Simulated 10% failure rate
        if (Math.random() < 0.10) {
            throw new RuntimeException("Shipping service unavailable");
        }
        String trackingNumber = "TRK-" + System.currentTimeMillis();
        System.out.println("    → Shipping to: " + address);
        return trackingNumber;
    }

    @Override
    public String checkDeliveryStatus(String trackingNumber) {
        // BUG: Math.random() is non-deterministic - breaks Temporal replay!
        double rand = Math.random();
        if (rand < 0.3) return "in_transit";
        if (rand < 0.6) return "out_for_delivery";
        return "delivered";
    }

    @Override
    public void sendNotification(String email, String orderId, String status) {
        // 5% failure rate (non-critical)
        if (Math.random() < 0.05) {
            throw new RuntimeException("Email service timeout");
        }
        System.out.println("    → Email to " + email + ": " + status);
    }

    @Override
    public void refundPayment(String paymentId) {

    }

    @Override
    public void releaseInventory(String reservationId) {

    }
}
