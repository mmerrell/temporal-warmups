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
 *   Step 1: validatePayment   — local activity (same pattern as previous exercises)
 *   Step 2: checkCompliance   — Nexus SYNC call to the Compliance team  ← NEW
 *   Step 3: executePayment    — local activity (only if compliance approved)
 *
 * You need three class-level fields:
 *
 *   1. ACTIVITY_OPTIONS — configure timeout and retry policy for activities.
 *      A 30-second startToCloseTimeout and 3 max attempts is a good starting point.
 *
 *   2. paymentActivity stub — create it with Workflow.newActivityStub().
 *      This is the same pattern you've used since Exercise 01.
 *
 *   3. complianceService Nexus stub — create it with Workflow.newNexusServiceStub().
 *      METAPHOR: Same idea as creating an HTTP client, but durable.
 *      You give it the service interface class and a schedule-to-close timeout.
 *      It looks like a local Java object but routes calls across team boundaries.
 *
 * Where does "compliance-endpoint" come from?
 *   Not here — you configure the endpoint name in PaymentsWorkerApp.
 *   This workflow only knows the SERVICE (ComplianceNexusService).
 *   The worker knows the ENDPOINT. This keeps the workflow portable.
 *
 * In processPayment():
 *   Step 1 — Call validatePayment. If false, return a REJECTED result immediately.
 *   Step 2 — Build a ComplianceRequest from the PaymentRequest fields, call the
 *             Nexus stub. If compliance.isApproved() is false, return DECLINED_COMPLIANCE.
 *             The riskLevel and explanation from the compliance result belong in
 *             the final PaymentResult — save them.
 *   Step 3 — Call executePayment. Return a COMPLETED result with the confirmation number.
 *   Wrap everything in try/catch and return a FAILED result on unexpected errors.
 *
 * Logging: Use Workflow.getLogger(), never System.out.println().
 *   Workflows replay on worker restart. println fires on every replay.
 *   Workflow.getLogger() is replay-safe — logs only on first execution.
 *
 * In Exercise 1300, this workflow also makes an ASYNC Nexus call and
 * pauses for a human approval signal via Workflow.await().
 */
public class PaymentProcessingWorkflowImpl implements PaymentProcessingWorkflow {

    // TODO: Create ACTIVITY_OPTIONS (startToCloseTimeout, RetryOptions)
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))  // how long can one attempt take?
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))  // wait 1s before first retry
                    .setBackoffCoefficient(2)                   // double the wait each time
                    .build())
            .build();

    // TODO: Create a paymentActivity stub using Workflow.newActivityStub()
    private final PaymentActivity paymentActivity =
            Workflow.newActivityStub(PaymentActivity.class, ACTIVITY_OPTIONS);

    // TODO: Create a complianceService Nexus stub using Workflow.newNexusServiceStub()
    private final ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
            ComplianceNexusService.class,
            NexusServiceOptions.newBuilder()
                    .setOperationOptions(NexusOperationOptions.newBuilder()
                            .setScheduleToCloseTimeout(Duration.ofMinutes(2))
                            .build())
                    .build());

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // TODO: Implement 3-step orchestration
        try {
            // Step 1: Validate payment (Payments team)
            boolean valid = paymentActivity.validatePayment(request);
            if (!valid) {
                System.out.println("  REJECTED: Validation failed — payment lost, no retry\n");
            }
            System.out.println("  Step 1 passed: validation OK");

            // Step 2: Call Compliance team for risk check
            // PROBLEM: Direct call — no retries, no durability, tight coupling
            ComplianceRequest compReq = new ComplianceRequest(
                    request.getTransactionId(), request.getAmount(),
                    request.getSenderCountry(), request.getReceiverCountry(),
                    request.getDescription());

            System.out.println("  Step 2: calling Compliance team...");
            ComplianceResult compliance = complianceService.checkCompliance(compReq);
            System.out.println("  Compliance result: " + compliance.getRiskLevel()
                    + " | approved=" + compliance.isApproved());
            System.out.println("  Reason: " + compliance.getExplanation());

            if (!compliance.isApproved()) {
                System.out.println("  DECLINED: Compliance blocked this transaction");
                System.out.println("  ** No audit trail — why was it declined? Who decided? **\n");
            }

            // Step 3: Execute payment
            // PROBLEM: If step 2 succeeded but this crashes, we don't know
            System.out.println("  Step 3: executing payment...");
            String confirmation = paymentActivity.executePayment(request);
            System.out.println("  COMPLETED: " + confirmation);
            return new PaymentResult(true, request.getTransactionId(), "COMPLETED",
                    compliance.getRiskLevel(),
                    compliance.getExplanation(),
                    confirmation,
                    null);

        } catch (Exception e) {
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .error("Workflow failed: " + e.getMessage());
            return new PaymentResult(false, request.getTransactionId(), "FAILED",
                    null, null, null, e.getMessage());
        }
    }
}
