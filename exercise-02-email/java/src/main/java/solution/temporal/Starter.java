package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.UUID;

public class Starter {
    //1. create a task queue
    private static final String TASK_QUEUE = "email-verification";

    //2. create main()
    public static void main(String[] args) {
        //3. also connects to Temporal like Worker
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Define test emails
        String[] emails = {"alice@example.com", "bob@example.com", "charlie@example.com"};

        // 3. For each email:
        for (String email : emails) {
            // Create a unique workflow ID (use UUID)
            String workflowId = "email-verification-" + UUID.randomUUID();

            // Create a workflow stub (typed to your interface!)
            //4. create a workflow stub with a queue and id
            EmailVerificationWorkflow workflow = client.newWorkflowStub(EmailVerificationWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .build());

            // Execute the workflow by calling run(email)
            // This blocks until the workflow completes!
            workflow.verifyEmail(email);

            // Print the result
            System.out.println("\n\nSummary:");
//            System.out.println("Emails sent: " + service.sentEmails.size());
        }

        System.exit(0);  // Exit cleanly when done
    }
}
