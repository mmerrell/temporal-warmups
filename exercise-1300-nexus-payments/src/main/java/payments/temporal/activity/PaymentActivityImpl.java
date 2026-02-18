package payments.temporal.activity;

import payments.PaymentGateway;
import payments.domain.PaymentRequest;

/**
 * [STUDENT IMPLEMENTS] Activity implementation for payment operations.
 *
 * Pattern: Thin wrapper that delegates to PaymentGateway business logic.
 */
public class PaymentActivityImpl implements PaymentActivity {

    private final PaymentGateway gateway;

    public PaymentActivityImpl(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean validatePayment(PaymentRequest request) {
        System.out.println("[PaymentActivity] Validating " + request.getTransactionId());
        return gateway.validatePayment(request);
    }

    @Override
    public String executePayment(PaymentRequest request) {
        System.out.println("[PaymentActivity] Executing " + request.getTransactionId());
        return gateway.executePayment(request);
    }
}
