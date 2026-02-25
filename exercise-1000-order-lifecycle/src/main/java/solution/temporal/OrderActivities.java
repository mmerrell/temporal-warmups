package solution.temporal;

import exercise.domain.Order;
import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface OrderActivities {
    void validateOrder(Order order);
    String processPayment(String orderId, String email, double amount);
    String reserveInventory(String orderId, List<Order.OrderItem> items);
    String createShipment(String orderId, String address);
    String checkDeliveryStatus(String trackingNumber);
    void sendNotification(String email, String orderId, String status);
    void refundPayment(String paymentId);
    void releaseInventory(String reservationId);
}
