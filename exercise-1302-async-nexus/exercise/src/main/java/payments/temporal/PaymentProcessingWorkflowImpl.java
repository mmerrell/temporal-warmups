package payments.temporal;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;
import payments.temporal.activity.PaymentActivity;
import shared.nexus.ComplianceNexusService;

import java.time.Duration;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  TODO FILE 5 of 7: PaymentProcessingWorkflowImpl — New API (20 min)
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: Use NexusOperationHandle on the calling side — the conceptual
 * partner to ComplianceNexusServiceImpl. Where File 4 starts the workflow,
 * this file holds the handle and awaits the result.
 *
 * ─── THE KEY API CHANGE FROM 1301 ────────────────────────────────
 *
 * In Exercise 1301 (sync):
 *   ComplianceResult result = complianceService.checkCompliance(compReq);
 *   // Blocks until compliance returns (fast in 1301, but breaks with slow pipelines)
 *
 * In Exercise 1302 (async):
 *   NexusOperationHandle<InvestigationResult> handle =
 *       Workflow.startNexusOperation(complianceService::investigate, investigationReq);
 *   // Returns immediately — Temporal holds the handle; compliance runs independently
 *   InvestigationResult result = handle.getResult().get();
 *   // Durable await — workflow suspends here until investigation completes
 *
 * TWO LINES instead of one. But the difference is enormous:
 *   - No thread is blocked waiting
 *   - If your worker crashes between startNexusOperation and getResult().get(),
 *     Temporal replays the workflow and re-attaches to the running investigation
 *   - You can start multiple handles in parallel (see Exercise 1300)
 *
 * ─── ORCHESTRATION STEPS ─────────────────────────────────────────
 *
 * TODO 1 — Define ACTIVITY_OPTIONS and stubs:
 *   - ACTIVITY_OPTIONS: startToCloseTimeout=30s, retryOptions maxAttempts=3
 *   - paymentActivity stub: Workflow.newActivityStub(PaymentActivity.class, ...)
 *   - complianceService stub: Workflow.newNexusServiceStub(ComplianceNexusService.class, ...)
 *     with NexusServiceOptions → NexusOperationOptions → scheduleToCloseTimeout=2min
 *
 * TODO 2 — Validate payment:
 *   boolean valid = paymentActivity.validatePayment(request);
 *   if (!valid) return new PaymentResult(false, request.getTransactionId(),
 *       "REJECTED", null, null, null, "Validation failed");
 *
 * TODO 3 — Build InvestigationRequest and start Nexus operation:
 *   InvestigationRequest investigationReq = new InvestigationRequest(
 *       request.getTransactionId(), request.getAmount(),
 *       request.getSenderCountry(), request.getReceiverCountry(),
 *       request.getDescription());
 *   NexusOperationHandle<InvestigationResult> handle =
 *       Workflow.startNexusOperation(complianceService::investigate, investigationReq);
 *
 * TODO 4 — Await the investigation result:
 *   InvestigationResult investigation = handle.getResult().get();
 *   if (investigation.isBlocked()) {
 *       return new PaymentResult(false, request.getTransactionId(),
 *           "BLOCKED_COMPLIANCE", investigation.getRiskLevel(),
 *           investigation.getSummary(), null, null);
 *   }
 *
 * TODO 5 — Execute payment and return COMPLETED:
 *   String confirmation = paymentActivity.executePayment(request);
 *   return new PaymentResult(true, request.getTransactionId(),
 *       "COMPLETED", investigation.getRiskLevel(),
 *       investigation.getSummary(), confirmation, null);
 *
 * WRAP ALL STEPS in try/catch — return a FAILED result on unexpected error.
 *
 * LOGGING: Use Workflow.getLogger(PaymentProcessingWorkflowImpl.class), not System.out.println().
 */
public class PaymentProcessingWorkflowImpl implements PaymentProcessingWorkflow {

    // TODO 1a: Define ACTIVITY_OPTIONS (startToCloseTimeout=30s, retryOptions)
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    // TODO 1b: Create paymentActivity stub using Workflow.newActivityStub(...)
    // Hint: Workflow.newActivityStub(PaymentActivity.class, ACTIVITY_OPTIONS)

    // TODO 1c: Create complianceService Nexus stub using Workflow.newNexusServiceStub(...)
    // Hint: Workflow.newNexusServiceStub(ComplianceNexusService.class,
    //           NexusServiceOptions.newBuilder()
    //               .setOperationOptions(NexusOperationOptions.newBuilder()
    //                   .setScheduleToCloseTimeout(Duration.ofMinutes(2))
    //                   .build())
    //               .build())

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            // TODO 2: Validate payment — return REJECTED if false

            // TODO 3: Build InvestigationRequest and call Workflow.startNexusOperation(...)
            //         Store the returned NexusOperationHandle<InvestigationResult>

            // TODO 4: Call handle.getResult().get() to await result
            //         Return BLOCKED_COMPLIANCE if investigation.isBlocked()

            // TODO 5: Execute payment and return COMPLETED

            return null; // Remove this line once you implement the steps above

        } catch (Exception e) {
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .error("Workflow failed for " + request.getTransactionId() + ": " + e.getMessage());
            return new PaymentResult(false, request.getTransactionId(), "FAILED",
                    null, null, null, e.getMessage());
        }
    }
}
