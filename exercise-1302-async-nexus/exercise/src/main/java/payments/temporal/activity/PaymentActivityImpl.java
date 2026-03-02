package payments.temporal.activity;

import payments.PaymentGateway;
import payments.domain.PaymentRequest;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  TODO FILE 1 of 7: PaymentActivityImpl — Warm-Up (5 min)
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: Muscle memory. Thin wrapper that delegates to PaymentGateway.
 * Same pattern you've used since Exercise 01.
 *
 * WHAT TO IMPLEMENT:
 *
 *   1. Add a private field: PaymentGateway gateway
 *
 *   2. Add a constructor that accepts a PaymentGateway and assigns it
 *      to the field.
 *
 *   3. Implement validatePayment():
 *      → return gateway.validatePayment(request)
 *
 *   4. Implement executePayment():
 *      → return gateway.executePayment(request)
 *
 * PATTERN REMINDER:
 *   Activities are the "thin wrappers" that bridge Temporal to business logic.
 *   The activity doesn't contain business logic — it delegates to the gateway.
 *   This keeps PaymentGateway testable without Temporal.
 *
 * WHEN YOU'RE DONE:
 *   This file doesn't have its own checkpoint — it's used by PaymentProcessingWorkflowImpl.
 *   You'll test it in Checkpoint 3 when you run the full workflow.
 */
public class PaymentActivityImpl implements PaymentActivity {

    // TODO 1: Add private PaymentGateway field

    // TODO 2: Add constructor that accepts PaymentGateway
    // (The no-arg constructor below is a temporary placeholder so the project compiles.
    //  Replace it with: public PaymentActivityImpl(PaymentGateway gateway))
    public PaymentActivityImpl() {
        // Placeholder — replace with parameterized constructor
    }

    @Override
    public boolean validatePayment(PaymentRequest request) {
        return false; // TODO 3: delegate to gateway.validatePayment(request)
    }

    @Override
    public String executePayment(PaymentRequest request) {
        return null; // TODO 4: delegate to gateway.executePayment(request)
    }
}
