package payments.temporal.activity;

import payments.PaymentGateway;
import payments.domain.PaymentRequest;

/**
 * YOUR TURN: Implement the payment activity.
 *
 * This is the easiest file in the exercise.
 * The pattern: accept dependencies via constructor, delegate to them.
 *
 * What to implement:
 *   1. Add a PaymentGateway field
 *   2. Accept PaymentGateway via constructor
 *   3. validatePayment → delegate to gateway
 *   4. executePayment  → delegate to gateway
 *
 * Why use activities at all?
 *   PaymentGateway.executePayment() has a 10% chance of throwing an exception
 *   (simulating a real gateway outage). Without Temporal, you'd write retry loops.
 *   With activities, Temporal retries automatically based on the RetryOptions
 *   you configure in the workflow. No retry logic needed here.
 */
public class PaymentActivityImpl implements PaymentActivity {

    // TODO: Add a PaymentGateway field

    // TODO: Add a constructor that accepts PaymentGateway

    @Override
    public boolean validatePayment(PaymentRequest request) {
        // TODO: Delegate to gateway
        throw new UnsupportedOperationException("TODO: implement validatePayment");
    }

    @Override
    public String executePayment(PaymentRequest request) {
        // TODO: Delegate to gateway
        throw new UnsupportedOperationException("TODO: implement executePayment");
    }
}
