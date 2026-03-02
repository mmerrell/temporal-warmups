package payments.temporal.activity;

import payments.PaymentGateway;
import payments.domain.PaymentRequest;

/**
 * [SOLUTION] Thin activity wrapper that delegates to PaymentGateway.
 * The activity doesn't contain business logic — it bridges Temporal to the gateway.
 */
public class PaymentActivityImpl implements PaymentActivity {

    private final PaymentGateway gateway;

    public PaymentActivityImpl(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean validatePayment(PaymentRequest request) {
        return gateway.validatePayment(request);
    }

    @Override
    public String executePayment(PaymentRequest request) {
        return gateway.executePayment(request);
    }
}
