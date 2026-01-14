package solution.temporal;

import exercise.model.ReviewRequest;
import exercise.model.ReviewResponse;
import exercise.model.TestSummary;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.UUID;

public class Starter {
    //1. create a task queue
    private static String TASK_QUEUE = "pr-review";

    //2. create main
    public static void main(String[] args)
    {
        //3. connect to temporal server, same as Worker
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        //4. create workflowId
        String workflowId = TASK_QUEUE + "-" + UUID.randomUUID();
        //5. create a workflow stub
        PRReviewWorkflow workflow = client.newWorkflowStub(PRReviewWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        //6. call the workflow
        // Dummy for demo purposes
        ReviewRequest request = createSampleRequest();
        ReviewResponse response = workflow.review(request);
    }

    private static ReviewRequest createSampleRequest() {
        ReviewRequest request = new ReviewRequest();
        request.prTitle = "Add fake pr";
        request.prDescription = "A dummy PR description";
        request.diff = "+ String APIKEY='exposed-key-123'";

        TestSummary testSummary = new TestSummary();
        testSummary.passed = true;
        testSummary.totalTests = 5;
        testSummary.failedTests = 0;
        testSummary.durationMs = 120;
        request.testSummary = testSummary;

        return request;
    }
}
