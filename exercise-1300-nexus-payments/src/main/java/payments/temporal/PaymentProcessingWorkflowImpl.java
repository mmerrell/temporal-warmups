package payments.temporal;

import compliance.domain.CategoryRequest;
import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import compliance.domain.TransactionCategory;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import payments.domain.ApprovalDecision;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;
import payments.temporal.activity.PaymentActivity;
import shared.nexus.ComplianceNexusService;

import java.time.Duration;

/**
 * YOUR TURN: Implement the main payment processing workflow.
 *
 * This is the BIG one — it combines everything from previous exercises
 * plus the new Nexus concept:
 *
 *   Step 1: Validate payment .............. local activity (same as Ex 01-04)
 *   Step 2: Categorize transaction ........ Nexus SYNC call → Compliance team (NEW!)
 *   Step 3: Screen for fraud .............. Nexus ASYNC call → Compliance team (NEW!)
 *   Step 4: Wait for human approval ....... Signal + Workflow.await() (same as Ex 06)
 *   Step 5: Execute payment ............... local activity (same as Ex 01-04)
 *
 * ═══════════════════════════════════════════════════════════════════
 *  KEY NEW CONCEPT: Nexus Service Stub
 * ═══════════════════════════════════════════════════════════════════
 *
 * Think of it like creating a REST client — but durable and type-safe:
 *
 *   ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
 *       ComplianceNexusService.class,
 *       NexusServiceOptions.newBuilder()
 *           .setOperationOptions(NexusOperationOptions.newBuilder()
 *               .setScheduleToCloseTimeout(Duration.ofMinutes(5))
 *               .build())
 *           .build());
 *
 * Then call it like any local method:
 *
 *   // SYNC call (returns immediately):
 *   TransactionCategory cat = complianceService.categorizeTransaction(catReq);
 *
 *   // ASYNC call (starts a workflow on the Compliance side):
 *   NexusOperationHandle<RiskScreeningResult> handle =
 *       Workflow.startNexusOperation(complianceService::screenTransaction, screenReq);
 *   RiskScreeningResult result = handle.getResult().get();
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SIGNAL PATTERN (from Exercise 06)
 * ═══════════════════════════════════════════════════════════════════
 *
 * For high-risk transactions, pause and wait for human approval:
 *   - Track signal state with instance fields (approvalReceived, approved, etc.)
 *   - Use Workflow.await(timeout, () -> approvalReceived) to wait
 *   - The @SignalMethod sets the fields when a signal arrives
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class PaymentProcessingWorkflowImpl implements PaymentProcessingWorkflow {

    // ── Signal state for human-in-the-loop (Exercise 06 pattern) ──
    // TODO: Add fields to track approval state:
       private boolean approvalReceived = false;
       private boolean approved = false;
       private String reviewerName = "";
       private String approvalReason = "";

    // ── Activity stub for local payment operations ──
    // TODO: Create ActivityOptions with:
    //   - startToCloseTimeout: 30 seconds
    //   - retryOptions: initialInterval=1s, backoff=2, maxAttempts=5
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))  // How long can one attempt take?
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5)) // How long before first retry? 2 sec is better for LLM
                    .setBackoffCoefficient(2)   // Multiply wait time by what?
                    .build())
            .build();
    // TODO: Create activity stub:
   private final PaymentActivity paymentActivity =
       Workflow.newActivityStub(PaymentActivity.class, ACTIVITY_OPTIONS);

    // ── Nexus service stub for calling the Compliance team's service ──
    // Think of this like creating a REST client, but durable and type-safe.
    // Instead of: new HttpClient("http://compliance-service/api/...")
    // Temporal gives us: Workflow.newNexusServiceStub(ComplianceNexusService.class, options)
    //
    // The stub looks like a local Java object, but calls are routed
    // across team boundaries via Temporal's Nexus infrastructure.

    // How long can a Nexus operation take before Temporal gives up?
    private static final NexusOperationOptions NEXUS_OPERATION_OPTIONS = NexusOperationOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofMinutes(5))
            .build();

    // Wire the operation options into the service-level config
    private static final NexusServiceOptions NEXUS_SERVICE_OPTIONS = NexusServiceOptions.newBuilder()
            .setOperationOptions(NEXUS_OPERATION_OPTIONS)
            .build();

    // Create the stub — now we can call complianceService.categorizeTransaction()
    // and complianceService.screenTransaction() as if they were local methods
    private final ComplianceNexusService complianceService =
            Workflow.newNexusServiceStub(ComplianceNexusService.class, NEXUS_SERVICE_OPTIONS);


    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Use Workflow.getLogger() — NOT System.out.println()!
        Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                .info("Payment workflow started for: " + request.getTransactionId());

        try {
            // ════════════════════════════════════════════════════
            // Step 1: Validate payment (local activity)
            // ════════════════════════════════════════════════════
            // TODO: Call paymentActivity.validatePayment(request)
            //   If invalid, return PaymentResult(false, txnId, "REJECTED", ...)
            boolean isValid = paymentActivity.validatePayment(request);
            if(!isValid){
                return new PaymentResult(false, request.transactionId, "REJECTED",
                        "", "", "", "");
            }
            // ════════════════════════════════════════════════════
            // Step 2: Categorize transaction via Nexus (SYNC)
            // ════════════════════════════════════════════════════
            // TODO:
            //   1. Build a CategoryRequest from the PaymentRequest fields:

                  CategoryRequest categoryRequest = new CategoryRequest(request.transactionId,
                          request.getAmount(), request.getDescription(), request.getSenderCountry(),
                          request.getReceiverCountry());
            //   2. Call: complianceService.categorizeTransaction(catReq)
            //      This is a SYNC Nexus call — it returns immediately.
            TransactionCategory transactionCategory = complianceService.categorizeTransaction(categoryRequest);
            //   3. Log the result
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .info(transactionCategory.toString());

            // ════════════════════════════════════════════════════
            // Step 3: Screen for fraud via Nexus (ASYNC)
            // ════════════════════════════════════════════════════
            // TODO:
            //   1. Build a RiskScreeningRequest from the PaymentRequest fields

            //   2. Start an ASYNC Nexus operation:
            //      NexusOperationHandle<RiskScreeningResult> handle =
            //          Workflow.startNexusOperation(complianceService::screenTransaction, screenReq);
            //   3. Wait for the result:
            //      RiskScreeningResult riskResult = handle.getResult().get();
            //   4. Log the risk level and score
            //
            // WHY ASYNC? Fraud detection starts a full FraudDetectionWorkflow
            // on the Compliance side. It could take minutes (AI analysis).
            // The handle lets us track it and get the result when it's done.

            // ════════════════════════════════════════════════════
            // Step 4: Human approval for high-risk (Signal pattern)
            // ════════════════════════════════════════════════════
            // TODO:
            //   if (riskResult.isRequiresApproval()) {
            //       1. Log that we're waiting for approval
            //       2. Wait: boolean received = Workflow.await(Duration.ofHours(24), () -> approvalReceived);
            //       3. If !received → return timeout result
            //       4. If !approved → return rejected result
            //       5. If approved → log and continue to step 5
            //   }

            // ════════════════════════════════════════════════════
            // Step 5: Execute payment (local activity)
            // ════════════════════════════════════════════════════
            // TODO: Call paymentActivity.executePayment(request)
            //   Return PaymentResult(true, txnId, "COMPLETED", riskLevel, category, confirmationNumber, null)

            throw new UnsupportedOperationException("Implement the 5 steps above!");

        } catch (Exception e) {
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .error("Payment failed: " + e.getMessage());
            return new PaymentResult(false, request.getTransactionId(), "FAILED",
                    null, null, null, e.getMessage());
        }
    }

    @Override
    public void approveTransaction(ApprovalDecision decision) {
        // TODO: Set the signal state fields:
        //   this.approved = decision.isApproved();
        //   this.reviewerName = decision.getReviewerName();
        //   this.approvalReason = decision.getReason();
        //   this.approvalReceived = true;
        //
        // REMEMBER: Signal methods must return void.
        // The Workflow.await() in step 4 will unblock when approvalReceived becomes true.
    }
}
