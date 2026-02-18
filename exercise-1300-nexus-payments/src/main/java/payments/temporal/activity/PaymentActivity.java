package payments.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import payments.domain.PaymentRequest;

/**
 * [STUDENT IMPLEMENTS] Activity interface for payment operations.
 *
 * Why are these activities?
 * - validatePayment: Could call external validation service
 * - executePayment: Calls payment gateway (external, can fail)
 * Both are non-deterministic and should be retried by Temporal.
 */
@ActivityInterface
public interface PaymentActivity {

    @ActivityMethod
    boolean validatePayment(PaymentRequest request);

    @ActivityMethod
    String executePayment(PaymentRequest request);
}
