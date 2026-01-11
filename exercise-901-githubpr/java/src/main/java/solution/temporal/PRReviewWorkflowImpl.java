package solution.temporal;

import exercise.model.ReviewRequest;
import exercise.model.ReviewResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class PRReviewWorkflowImpl implements PRReviewWorkflow {
    // 1. configure how activities should behave
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))  // How long can one attempt take?
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5)) // How long before first retry? 2 sec is better for LLM
                    .setBackoffCoefficient(2)   // Multiply wait time by what?
                    .build())
            .build();

    //2. create activity stubs with non-existent @ActivityInterface. Using Workflow
    private final CodeQualityActivity codeQualityActivity = Workflow.newActivityStub(
            CodeQualityActivity.class, ACTIVITY_OPTIONS
    );
    private final TestQualityActivity testQualityActivity = Workflow.newActivityStub(
            TestQualityActivity.class, ACTIVITY_OPTIONS
    );
    private final SecurityQualityActivity securityQualityActivity = Workflow.newActivityStub(
            SecurityQualityActivity.class, ACTIVITY_OPTIONS
    );

    @Override
    public ReviewResponse review(ReviewRequest request) {
        return null;
    }
}
