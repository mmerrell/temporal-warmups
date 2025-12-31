package solution.temporal;

import solution.domain.User;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Starter {
    //1. copy the task queue from the Worker
    private static final String TASK_QUEUE = "registration";

    //2. also needs an entry point to start the workflow
    public static void main(String[] args) throws InterruptedException {
        //3. also connects to Temporal like Worker
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        //4. create a workflow stub with a queue and id
        RegistrationWorkflow workflow = client.newWorkflowStub(RegistrationWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId("registrationg-flow")
                        .build()
        );

        //5. call the workflow
        User user1 = new User("alice@example.com", "alice", "secure123");
        User user2 = new User("bob@example.com", "bob", "secure123");
        User user3 = new User("todd@example.com", "todd", "unsecure");
        workflow.registerUser(user1);
        workflow.registerUser(user2);
        workflow.registerUser(user3);
    }
}
