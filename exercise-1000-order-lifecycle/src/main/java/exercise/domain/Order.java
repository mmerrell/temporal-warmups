package exercise.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared domain model for orders.
 * Uses simple types and public fields for Temporal serialization compatibility.
 */
public class Order {

    public String orderId;
    public String customerEmail;
    public String customerAddress;
    public List<OrderItem> items;

    // Default constructor required for Temporal serialization
    public Order() {
        this.items = new ArrayList<>();
    }

    public Order(String orderId, String customerEmail, String customerAddress, List<OrderItem> items) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.customerAddress = customerAddress;
        this.items = items != null ? items : new ArrayList<>();
    }
    @JsonIgnore
    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(item -> item.price * item.quantity)
                .sum();
    }

    @Override
    public String toString() {
        return String.format("Order{id='%s', email='%s', items=%d, total=$%.2f}",
                orderId, customerEmail, items.size(), getTotalAmount());
    }

    /**
     * Represents a single item in an order.
     */
    public static class OrderItem {
        public String sku;
        public String name;
        public int quantity;
        public double price;

        // Default constructor required for Temporal serialization
        public OrderItem() {}

        public OrderItem(String sku, String name, int quantity, double price) {
            this.sku = sku;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        @Override
        public String toString() {
            return String.format("%s (x%d @ $%.2f)", name, quantity, price);
        }
    }
}
