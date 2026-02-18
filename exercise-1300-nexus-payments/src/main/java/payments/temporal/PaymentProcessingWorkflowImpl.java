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
 * [STUDENT IMPLEMENTS] Main payment processing workflow.
 *
 * This is the Payments team's workflow that:
 * 1. Validates payment (local activity)
 * 2. Calls Compliance team via Nexus for categorization (SYNC Nexus call)
 * 3. Calls Compliance team via Nexus for fraud screening (ASYNC Nexus call)
 * 4. If high risk: waits for human approval via Signal (Ex 06 pattern)
 * 5. Executes payment (local activity)
 *
 * KEY NEW CONCEPT: Workflow.newNexusServiceStub() to call cross-team services
 */
public class PaymentProcessingWorkflowImpl implements PaymentProcessingWorkflow {

    // Signal state for human-in-the-loop (same pattern as Exercise 06)
    private boolean approvalReceived = false;
    private boolean approved = false;
    private String reviewerName = "";
    private String approvalReason = "";

    // Activity options for local payment operations
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2)
                    .setMaximumAttempts(5)
                    .build())
            .build();

    // Local activity stub (Payments team)
    private final PaymentActivity paymentActivity = Workflow.newActivityStub(
            PaymentActivity.class, ACTIVITY_OPTIONS);

    // KEY NEXUS CONCEPT: Create a Nexus service stub to call the Compliance team
    // This is like creating a REST client, but durable and type-safe
    // Note: The endpoint mapping ("ComplianceNexusService" -> "compliance-endpoint")
    // is configured in the WORKER registration, not here. This keeps workflows portable.
    private final ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
            ComplianceNexusService.class,
            NexusServiceOptions.newBuilder()
                    .setOperationOptions(NexusOperationOptions.newBuilder()
                            .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                            .build())
                    .build());

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                .info("Payment workflow started for: " + request.getTransactionId()
                        + " | $" + String.format("%.2f", request.getAmount()));

        try {
            // ============================================================
            // Step 1: Validate payment (local activity - Payments team)
            // ============================================================
            boolean valid = paymentActivity.validatePayment(request);
            if (!valid) {
                return new PaymentResult(false, request.getTransactionId(), "REJECTED",
                        null, null, null, "Payment validation failed");
            }

            // ============================================================
            // Step 2: Categorize transaction via Nexus (SYNC - Compliance team)
            // ============================================================
            // This calls the Compliance team's categorizeTransaction() handler
            // via Nexus. It's a synchronous operation - returns immediately.
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .info("Calling Compliance team via Nexus for categorization...");

            CategoryRequest catReq = new CategoryRequest(
                    request.getTransactionId(), request.getAmount(),
                    request.getDescription(), request.getSenderCountry(),
                    request.getReceiverCountry());

            TransactionCategory category = complianceService.categorizeTransaction(catReq);

            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .info("Category: " + category.getCategory() + " (" + category.getSubCategory() + ")"
                            + " | Regulatory: " + category.getRegulatoryFlags());

            // ============================================================
            // Step 3: Screen for fraud via Nexus (ASYNC - Compliance team)
            // ============================================================
            // This starts a FraudDetectionWorkflow on the Compliance side.
            // Async because fraud detection is a long-running operation.
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .info("Calling Compliance team via Nexus for fraud screening...");

            RiskScreeningRequest screenReq = new RiskScreeningRequest(
                    request.getTransactionId(), request.getAmount(),
                    request.getSenderCountry(), request.getReceiverCountry(),
                    request.getDescription());

            // Async Nexus pattern: start the operation and get a handle
            // Then wait for the result. This starts a FraudDetectionWorkflow
            // on the Compliance side via Nexus.
            NexusOperationHandle<RiskScreeningResult> screenHandle =
                    Workflow.startNexusOperation(complianceService::screenTransaction, screenReq);
            RiskScreeningResult riskResult = screenHandle.getResult().get();

            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .info("Risk: " + riskResult.getRiskLevel() + " (score: " + riskResult.getRiskScore() + ")"
                            + " | Sanctions: " + riskResult.isFlaggedSanctions());

            // ============================================================
            // Step 4: Human approval for high-risk transactions (Signal pattern from Ex 06)
            // ============================================================
            if (riskResult.isRequiresApproval()) {
                Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                        .info("HIGH RISK - Waiting for human approval signal...");
                Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                        .info("Send signal: temporal workflow signal --workflow-id payment-"
                                + request.getTransactionId()
                                + " --name approveTransaction --input "
                                + "'{\"approved\":true,\"reviewerName\":\"Jane\",\"reason\":\"Verified\"}'");

                // Wait for approval signal (same pattern as Exercise 06)
                boolean signalReceived = Workflow.await(
                        Duration.ofHours(24),
                        () -> approvalReceived
                );

                if (!signalReceived) {
                    return new PaymentResult(false, request.getTransactionId(), "TIMEOUT",
                            riskResult.getRiskLevel(), category.getCategory(), null,
                            "No approval received within 24 hours");
                }

                if (!approved) {
                    return new PaymentResult(false, request.getTransactionId(), "REJECTED",
                            riskResult.getRiskLevel(), category.getCategory(), null,
                            "Rejected by " + reviewerName + ": " + approvalReason);
                }

                Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                        .info("Approved by " + reviewerName + ": " + approvalReason);
            }

            // ============================================================
            // Step 5: Execute payment (local activity - Payments team)
            // ============================================================
            String confirmationNumber = paymentActivity.executePayment(request);

            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .info("Payment completed: " + confirmationNumber);

            return new PaymentResult(true, request.getTransactionId(), "COMPLETED",
                    riskResult.getRiskLevel(), category.getCategory(), confirmationNumber, null);

        } catch (Exception e) {
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .error("Payment failed: " + e.getMessage());
            return new PaymentResult(false, request.getTransactionId(), "FAILED",
                    null, null, null, e.getMessage());
        }
    }

    @Override
    public void approveTransaction(ApprovalDecision decision) {
        this.approved = decision.isApproved();
        this.reviewerName = decision.getReviewerName();
        this.approvalReason = decision.getReason();
        this.approvalReceived = true;

        Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                .info("Approval signal received: " + (decision.isApproved() ? "APPROVED" : "REJECTED")
                        + " by " + decision.getReviewerName());
    }
}
