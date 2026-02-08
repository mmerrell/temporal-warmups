package solution.temporal.domain;

public class OrderTrackingInfo {

    public String orderId;
    public String currentStatus;
    public String trackingNumber;
    public String paymentId;
    public String reservationId;

    public OrderTrackingInfo() {}

    public OrderTrackingInfo(String orderId, String currentStatus, String trackingNumber, String paymentId, String reservationId) {
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.trackingNumber = trackingNumber;
        this.paymentId = paymentId;
        this.reservationId = reservationId;
    }

    @Override
    public String toString() {
        return String.format("OrderTrackingInfo{orderId='%s', status='%s', tracking='%s'}",
                orderId, currentStatus, trackingNumber);
    }
}
