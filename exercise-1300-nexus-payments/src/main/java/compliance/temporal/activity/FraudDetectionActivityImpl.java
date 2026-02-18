package compliance.temporal.activity;

import compliance.FraudDetectionAgent;
import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import io.temporal.activity.Activity;

/**
 * [STUDENT IMPLEMENTS] Activity implementation for fraud detection.
 *
 * Pattern: Activity delegates to business logic class (FraudDetectionAgent).
 * This keeps the activity thin - just a bridge between Temporal and business logic.
 */
public class FraudDetectionActivityImpl implements FraudDetectionActivity {

    private final FraudDetectionAgent fraudAgent;

    public FraudDetectionActivityImpl(FraudDetectionAgent fraudAgent) {
        this.fraudAgent = fraudAgent;
    }

    @Override
    public RiskScreeningResult screenTransaction(RiskScreeningRequest request) {
        Activity.getExecutionContext().getInfo();
        System.out.println("[FraudDetectionActivity] Screening transaction: " + request.getTransactionId());

        RiskScreeningResult result = fraudAgent.screenTransaction(request);

        System.out.println("[FraudDetectionActivity] Result for " + request.getTransactionId()
                + ": " + result.getRiskLevel() + " (score: " + result.getRiskScore() + ")");
        return result;
    }
}
