package payments.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;

/**
 * [GIVEN] Workflow interface for the payment processing workflow.
 * Implemented by you in PaymentProcessingWorkflowImpl.java.
 */
@WorkflowInterface
public interface PaymentProcessingWorkflow {
    @WorkflowMethod
    PaymentResult processPayment(PaymentRequest request);
}
