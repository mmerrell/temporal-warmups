package payments.temporal;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import payments.domain.ApprovalDecision;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;

/**
 * [STUDENT IMPLEMENTS] Workflow interface for payment processing.
 *
 * Combines concepts from previous exercises:
 * - Workflow orchestration (Ex 01-04)
 * - @SignalMethod for human approval (Ex 06)
 * - Business identifier workflow IDs (Ex 06a)
 *
 * NEW: Nexus service calls to Compliance team
 */
@WorkflowInterface
public interface PaymentProcessingWorkflow {

    @WorkflowMethod
    PaymentResult processPayment(PaymentRequest request);

    /**
     * Signal for human approval of high-risk transactions.
     * Called via CLI: temporal workflow signal --workflow-id payment-TXN-002 \
     *   --name approveTransaction --input '{"approved":true,"reviewerName":"Jane","reason":"Verified"}'
     */
    @SignalMethod
    void approveTransaction(ApprovalDecision decision);
}
