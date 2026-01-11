package solution.temporal;

import exercise.model.AgentResult;
import exercise.model.Metadata;
import exercise.model.ReviewRequest;
import exercise.model.ReviewResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

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
        // Use this instead of System.currentTimeMillis()
        long startMs = Workflow.currentTimeMillis();

        System.out.println("=".repeat(60));
        System.out.println("Starting PR Review: " + request.prTitle);
        System.out.println("=".repeat(60));

        try {
            // 1. Call Code Quality Agent (BLOCKS for ~2-3 seconds)
            System.out.println("[1/3] Calling Code Quality Agent...");
            AgentResult codeQuality = codeQualityActivity.analyze(
                    request
            );
            System.out.println("      → " + codeQuality.recommendation + " (Risk: " + codeQuality.riskLevel + ")");

            // 2. Call Test Quality Agent (BLOCKS for ~2-3 seconds)
            System.out.println("[2/3] Calling Test Quality Agent...");
            AgentResult testQuality = testQualityActivity.analyze(
                request
            );
            System.out.println("      → " + testQuality.recommendation + " (Risk: " + testQuality.riskLevel + ")");

            // 3. Call Security Agent (BLOCKS for ~2-3 seconds)
            System.out.println("[3/3] Calling Security Agent...");
            AgentResult security = securityQualityActivity.analyze(
                request
            );
            System.out.println("      → " + security.recommendation + " (Risk: " + security.riskLevel + ")");

            // 4. Aggregate results
            String overall = aggregate(codeQuality, testQuality, security);

            // 5. Build response
            // Use this instead of System.currentTimeMillis()
            long tookMs = Workflow.currentTimeMillis() - startMs;

            Metadata metadata = new Metadata(
//                    Instant.now().toString(),
                    Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString(),
                    tookMs,
                    System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini")
            );

            ReviewResponse response = new ReviewResponse(
                    overall,
                    Arrays.asList(codeQuality, testQuality, security),
                    metadata
            );

            System.out.println("=".repeat(60));
            System.out.println("Review Complete: " + overall + " (took " + tookMs + "ms)");
            System.out.println("=".repeat(60));

            return response;

        } catch (Exception e) {
            // Fail fast - no retry logic, no graceful degradation
            System.err.println("Review failed: " + e.getMessage());
            throw new RuntimeException("Review orchestration failed", e);
        }

    }
    private String aggregate(AgentResult... results) {
        // Check for BLOCK
        for (AgentResult result : results) {
            if ("BLOCK".equals(result.recommendation)) {
                return "BLOCK";
            }
        }

        // Check for REQUEST_CHANGES
        for (AgentResult result : results) {
            if ("REQUEST_CHANGES".equals(result.recommendation)) {
                return "REQUEST_CHANGES";
            }
        }

        // All agents approved
        return "APPROVE";
    }
}
