package payments.temporal.activity;

import payments.PaymentGateway;
import payments.domain.PaymentRequest;

/**
 * YOUR TURN: Implement the payment activity.
 *
 * Same thin-wrapper pattern as FraudDetectionActivityImpl:
 *   1. Accept PaymentGateway via constructor
 *   2. Delegate to gateway methods
 *
 * Two methods to implement:
 *   - validatePayment → gateway.validatePayment(request)
 *   - executePayment  → gateway.executePayment(request)
 *
 * HINT: This is the easiest file. Just wire through to the gateway.
 */
public class PaymentActivityImpl implements PaymentActivity {

    // Store PaymentGateway as a field (dependency injection pattern)
    private final PaymentGateway gateway;

    public PaymentActivityImpl(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean validatePayment(PaymentRequest request) {
        // TODO: Delegate to gateway.validatePayment(request)
        return gateway.validatePayment(request);
    }

    @Override
    public String executePayment(PaymentRequest request) {
        // TODO: Delegate to gateway.executePayment(request)
        return gateway.executePayment(request);
    }
}
