package solution.temporal;

import exercise.model.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import solution.temporal.model.*;
import solution.temporal.workflow.BatchPRReviewWorkflow;

import java.util.*;

/**
 * Client that starts the batch workflow.
 *
 * Q: Why is this separate from the worker?
 * A: Separation of concerns:
 *    - Worker: Executes tasks (long-running process)
 *    - Client: Starts workflows (run once, exit)
 */
public class BatchStarter {

    /**
     * Adjust this to test continue-as-new behavior:
     *   10 PRs  → Probably no continue-as-new needed
     *   50 PRs  → Might trigger continue-as-new
     *   100 PRs → Will definitely trigger continue-as-new
     */
    private static final int NUM_PRS = 400;

    public static void main(String[] args) {
        // 1. Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Generate sample PRs
        List<ReviewRequest> prs = generateSamplePRs(NUM_PRS);
        String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
        BatchRequest request = new BatchRequest(prs, batchId);

        // 3. Create workflow stub
        String workflowId = "batch-pr-review-" + batchId;

        /**
         * Q: Why set a specific workflowId?
         * A: Makes it easy to find in the UI.
         *    Also prevents duplicate workflows if you run the starter twice.
         */
        BatchPRReviewWorkflow workflow = client.newWorkflowStub(
                BatchPRReviewWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(Shared.TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build()
        );

        System.out.println("Starting batch: " + workflowId);
        System.out.println("PRs to process: " + NUM_PRS);
        System.out.println("View at: http://localhost:8233/namespaces/default/workflows/" + workflowId);
        System.out.println();

        /**
         * Q: Why pass null as the second argument?
         * A: This is the FIRST run. No previous state exists.
         *    On continue-as-new, Temporal automatically passes the state.
         */
        BatchResult result = workflow.processBatch(request, null);

        // 5. Print results
        System.out.println("\n=== BATCH COMPLETE ===");
        System.out.println("Batch ID:      " + result.batchId);
        System.out.println("Total PRs:     " + result.totalCount);
        System.out.println("Succeeded:     " + result.successCount);
        System.out.println("Failed:        " + result.failureCount);
        System.out.println("Continuations: " + result.continuationCount);
        System.out.println("Duration:      " + result.totalDurationMs + "ms");
    }

    /**
     * Generate sample PRs for testing.
     */
    private static List<ReviewRequest> generateSamplePRs(int count) {
        List<ReviewRequest> prs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ReviewRequest pr = new ReviewRequest();
            pr.prTitle = "PR-" + (i + 1) + ": Sample change";
            pr.prDescription = "Test PR for batch processing";
            pr.diff = "+ // Change " + i;
            pr.testSummary = new TestSummary(true, 5, 0, 100);
            prs.add(pr);
        }
        return prs;
    }
}