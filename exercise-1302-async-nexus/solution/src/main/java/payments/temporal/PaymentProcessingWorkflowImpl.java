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
 * [SOLUTION] Orchestrates 5 steps: validate → start investigation → await result → check → execute.
 *
 * The key pattern: Workflow.startNexusOperation() returns immediately with a handle.
 * handle.getResult().get() is the durable await — the workflow suspends here until
 * the investigation workflow completes on the Compliance side.
 *
 * If the Payments worker crashes between startNexusOperation() and get(), Temporal
 * replays the workflow, re-attaches to the in-progress investigation, and continues.
 */
public class PaymentProcessingWorkflowImpl implements PaymentProcessingWorkflow {

    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final PaymentActivity paymentActivity =
            Workflow.newActivityStub(PaymentActivity.class, ACTIVITY_OPTIONS);

    private final ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
            ComplianceNexusService.class,
            NexusServiceOptions.newBuilder()
                    .setOperationOptions(NexusOperationOptions.newBuilder()
                            .setScheduleToCloseTimeout(Duration.ofMinutes(2))
                            .build())
                    .build());

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            // Step 1: Validate payment
            boolean valid = paymentActivity.validatePayment(request);
            if (!valid) {
                return new PaymentResult(false, request.getTransactionId(), "REJECTED",
                        null, null, null, "Validation failed");
            }

            // Step 2: Build investigation request and start async Nexus operation
            InvestigationRequest investigationReq = new InvestigationRequest(
                    request.getTransactionId(), request.getAmount(),
                    request.getSenderCountry(), request.getReceiverCountry(),
                    request.getDescription());

            NexusOperationHandle<InvestigationResult> handle =
                    Workflow.startNexusOperation(complianceService::investigate, investigationReq);

            // Step 3: Durably await the investigation result
            // Workflow suspends here — no thread blocked, fully durable
            InvestigationResult investigation = handle.getResult().get();

            // Step 4: Check investigation outcome
            if (investigation.isBlocked()) {
                return new PaymentResult(false, request.getTransactionId(),
                        "BLOCKED_COMPLIANCE", investigation.getRiskLevel(),
                        investigation.getSummary(), null, null);
            }

            // Step 5: Execute payment
            String confirmation = paymentActivity.executePayment(request);
            return new PaymentResult(true, request.getTransactionId(),
                    "COMPLETED", investigation.getRiskLevel(),
                    investigation.getSummary(), confirmation, null);

        } catch (Exception e) {
            Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                    .error("Workflow failed for " + request.getTransactionId() + ": " + e.getMessage());
            return new PaymentResult(false, request.getTransactionId(), "FAILED",
                    null, null, null, e.getMessage());
        }
    }
}
