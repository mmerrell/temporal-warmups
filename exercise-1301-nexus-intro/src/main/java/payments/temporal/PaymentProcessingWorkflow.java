package payments.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;

/**
 * [GIVEN] Workflow interface for payment processing.
 *
 * Simple: one workflow method, no signals (those come in Exercise 1300).
 */
@WorkflowInterface
public interface PaymentProcessingWorkflow {

    @WorkflowMethod
    PaymentResult processPayment(PaymentRequest request);
}
