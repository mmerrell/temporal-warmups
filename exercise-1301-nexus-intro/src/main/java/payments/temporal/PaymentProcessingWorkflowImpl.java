package payments.temporal;

import compliance.domain.ComplianceRequest;
import compliance.domain.ComplianceResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;
import payments.temporal.activity.PaymentActivity;
import shared.nexus.ComplianceNexusService;

import java.time.Duration;

/**
 * YOUR TURN: Implement the payment processing workflow.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  THIS IS THE KEY FILE IN THIS EXERCISE
 * ═══════════════════════════════════════════════════════════════════
 *
 * Orchestrate 3 steps:
 *
 *   Step 1: validatePayment   — local activity (same as previous exercises)
 *   Step 2: checkCompliance   — Nexus SYNC call to Compliance team ← NEW!
 *   Step 3: executePayment    — local activity (only if compliance approved)
 *
 * ── KEY NEW CONCEPT: Nexus Service Stub ──────────────────────────
 *
 * Think of it like creating a REST client, but type-safe and durable:
 *
 *   // Old way (fragile REST):
 *   HttpClient.post("http://compliance-service/check", request);
 *
 *   // New way (durable Nexus):
 *   ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
 *       ComplianceNexusService.class,
 *       NexusServiceOptions.newBuilder()
 *           .setOperationOptions(NexusOperationOptions.newBuilder()
 *               .setScheduleToCloseTimeout(Duration.ofMinutes(2))
 *               .build())
 *           .build());
 *
 * Then call it like any local method — Temporal handles the cross-team routing:
 *
 *   ComplianceResult result = complianceService.checkCompliance(request);
 *
 * The stub looks like a local Java object, but:
 *   - If the Compliance worker is down, Temporal retries the call
 *   - If YOUR worker crashes mid-call, replay picks up where it left off
 *   - The call appears in the Temporal UI as a linked Nexus operation
 *
 * ── Where does the "compliance-endpoint" come from? ──────────────
 *
 *   You don't configure it here — that happens in PaymentsWorkerApp.
 *   This workflow just knows the SERVICE (ComplianceNexusService).
 *   The worker knows the ENDPOINT ("compliance-endpoint").
 *   This separation keeps workflows portable.
 *
 * ── What to implement ────────────────────────────────────────────
 *
 *   1. Create ACTIVITY_OPTIONS (startToCloseTimeout: 30s, retries: 3 attempts)
 *
 *   2. Create paymentActivity stub:
 *      PaymentActivity paymentActivity = Workflow.newActivityStub(
 *          PaymentActivity.class, ACTIVITY_OPTIONS);
 *
 *   3. Create complianceService Nexus stub:
 *      ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
 *          ComplianceNexusService.class,
 *          NexusServiceOptions.newBuilder()
 *              .setOperationOptions(NexusOperationOptions.newBuilder()
 *                  .setScheduleToCloseTimeout(Duration.ofMinutes(2))
 *                  .build())
 *              .build());
 *
 *   4. In processPayment(), implement 3 steps:
 *
 *      Step 1 — validatePayment:
 *        boolean valid = paymentActivity.validatePayment(request);
 *        if (!valid) return new PaymentResult(false, txnId, "REJECTED", null, null, null, "Validation failed");
 *
 *      Step 2 — checkCompliance via Nexus:
 *        ComplianceRequest compReq = new ComplianceRequest(
 *            request.getTransactionId(), request.getAmount(),
 *            request.getSenderCountry(), request.getReceiverCountry(),
 *            request.getDescription());
 *        ComplianceResult compliance = complianceService.checkCompliance(compReq);
 *        if (!compliance.isApproved()) {
 *            return new PaymentResult(false, txnId, "DECLINED_COMPLIANCE",
 *                compliance.getRiskLevel(), compliance.getExplanation(), null,
 *                "Declined: " + compliance.getExplanation());
 *        }
 *
 *      Step 3 — executePayment:
 *        String conf = paymentActivity.executePayment(request);
 *        return new PaymentResult(true, txnId, "COMPLETED",
 *            compliance.getRiskLevel(), compliance.getExplanation(), conf, null);
 *
 *   5. Wrap steps in try/catch and return a FAILED result on unexpected errors.
 *
 *   6. Use Workflow.getLogger() for logging — NOT System.out.println()
 *      (workflows replay, so println would fire on every replay)
 *
 * ── In Exercise 1300, this workflow also ────────────────────────
 *   - Makes an ASYNC Nexus call that starts a FraudDetectionWorkflow
 *   - Waits for a human approval signal via Workflow.await()
 */
public class PaymentProcessingWorkflowImpl implements PaymentProcessingWorkflow {

    // TODO: Create ACTIVITY_OPTIONS
    // ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
    //     .setStartToCloseTimeout(Duration.ofSeconds(30))
    //     .setRetryOptions(RetryOptions.newBuilder()
    //         .setMaximumAttempts(3)
    //         .build())
    //     .build();

    // TODO: Create paymentActivity stub using Workflow.newActivityStub(...)

    // TODO: Create complianceService Nexus stub using Workflow.newNexusServiceStub(...)

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // TODO: Implement 3-step orchestration
        //   Step 1: Validate
        //   Step 2: checkCompliance via Nexus (the KEY new step)
        //   Step 3: Execute payment (only if compliance approved)
        throw new UnsupportedOperationException("TODO: implement processPayment");
    }
}
