package payments.temporal.activity;

import payments.PaymentGateway;
import payments.domain.PaymentRequest;

/**
 * YOUR TURN: Implement the payment activity.
 *
 * This is the easiest file in this exercise.
 * The pattern: accept dependencies via constructor, delegate to them.
 *
 * ── What to implement ────────────────────────────────────────────
 *
 *   1. Add a PaymentGateway field
 *   2. Accept PaymentGateway via constructor
 *   3. validatePayment → return gateway.validatePayment(request)
 *   4. executePayment  → return gateway.executePayment(request)
 *
 * HINT: This is the thin-wrapper pattern you've seen since Exercise 01.
 *       Business logic lives in PaymentGateway. Activities just delegate.
 *
 * ── Why use activities at all? ───────────────────────────────────
 *
 *   PaymentGateway.executePayment() has a 10% chance of throwing an exception
 *   (simulating a real gateway outage). Without Temporal, you'd write retry loops.
 *   With activities, Temporal retries automatically — no retry code needed here.
 */
public class PaymentActivityImpl implements PaymentActivity {

    // TODO: Add a PaymentGateway field
    // private final PaymentGateway gateway;

    // TODO: Accept PaymentGateway via constructor
    // public PaymentActivityImpl(PaymentGateway gateway) {
    //     this.gateway = gateway;
    // }

    @Override
    public boolean validatePayment(PaymentRequest request) {
        // TODO: return gateway.validatePayment(request)
        throw new UnsupportedOperationException("TODO: implement validatePayment");
    }

    @Override
    public String executePayment(PaymentRequest request) {
        // TODO: return gateway.executePayment(request)
        throw new UnsupportedOperationException("TODO: implement executePayment");
    }
}
