package solution.temporal.domain;

public class OrderResult {

    public String orderId;
    public String status;
    public String paymentId;
    public String trackingNumber;
    public String message;

    public OrderResult() {}

    public OrderResult(String orderId, String status, String paymentId, String trackingNumber, String message) {
        this.orderId = orderId;
        this.status = status;
        this.paymentId = paymentId;
        this.trackingNumber = trackingNumber;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("OrderResult{orderId='%s', status='%s', paymentId='%s', tracking='%s', message='%s'}",
                orderId, status, paymentId, trackingNumber, message);
    }
}
