package payments.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import payments.domain.PaymentRequest;

/**
 * [GIVEN] Activity interface for payment operations.
 *
 * Why are these activities (not regular Java methods)?
 *   validatePayment — could call an external validation service (non-deterministic)
 *   executePayment  — calls a payment gateway that can fail (non-deterministic)
 *
 * Temporal retries both automatically on failure, with the retry policy
 * configured in PaymentProcessingWorkflowImpl.
 */
@ActivityInterface
public interface PaymentActivity {

    @ActivityMethod
    boolean validatePayment(PaymentRequest request);

    @ActivityMethod
    String executePayment(PaymentRequest request);
}
