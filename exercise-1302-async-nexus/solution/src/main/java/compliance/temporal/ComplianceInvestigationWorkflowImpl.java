package compliance.temporal;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import compliance.temporal.activity.ComplianceInvestigationActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * [SOLUTION] Orchestrates the 3-phase investigation via the activity.
 *
 * Heartbeat timeout is shorter than any single investigation phase (~5-15s per phase),
 * so Temporal will detect a worker crash quickly and reschedule the activity.
 * This is what enables the durability demo in Checkpoint 4.
 */
public class ComplianceInvestigationWorkflowImpl implements ComplianceInvestigationWorkflow {

    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setHeartbeatTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final ComplianceInvestigationActivity activity =
            Workflow.newActivityStub(ComplianceInvestigationActivity.class, ACTIVITY_OPTIONS);

    @Override
    public InvestigationResult investigate(InvestigationRequest request) {
        Workflow.getLogger(ComplianceInvestigationWorkflowImpl.class)
                .info("Starting 3-phase investigation for " + request.getTransactionId());
        return activity.investigate(request);
    }
}
