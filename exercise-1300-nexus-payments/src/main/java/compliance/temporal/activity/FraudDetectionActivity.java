package compliance.temporal.activity;

import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * [STUDENT IMPLEMENTS] Activity interface for fraud detection.
 *
 * Why is this an activity?
 * - Calls external OpenAI API (non-deterministic)
 * - Can fail due to rate limits, network issues
 * - Temporal will retry automatically on failure
 */
@ActivityInterface
public interface FraudDetectionActivity {

    @ActivityMethod
    RiskScreeningResult screenTransaction(RiskScreeningRequest request);
}
