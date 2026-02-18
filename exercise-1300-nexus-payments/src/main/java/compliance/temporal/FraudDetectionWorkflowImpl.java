package compliance.temporal;

import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import compliance.temporal.activity.FraudDetectionActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * [STUDENT IMPLEMENTS] Workflow implementation for fraud detection.
 *
 * This is the Compliance team's workflow. It orchestrates the fraud
 * detection activity with proper retry policies.
 *
 * Called via Nexus from the Payments team's workflow.
 */
public class FraudDetectionWorkflowImpl implements FraudDetectionWorkflow {

    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setBackoffCoefficient(2)
                    .setMaximumAttempts(5)
                    .build())
            .build();

    private final FraudDetectionActivity fraudActivity = Workflow.newActivityStub(
            FraudDetectionActivity.class, ACTIVITY_OPTIONS);

    @Override
    public RiskScreeningResult screenTransaction(RiskScreeningRequest request) {
        Workflow.getLogger(FraudDetectionWorkflowImpl.class)
                .info("Fraud detection workflow started for: " + request.getTransactionId());

        RiskScreeningResult result = fraudActivity.screenTransaction(request);

        Workflow.getLogger(FraudDetectionWorkflowImpl.class)
                .info("Fraud detection complete for " + request.getTransactionId()
                        + ": " + result.getRiskLevel());

        return result;
    }
}
