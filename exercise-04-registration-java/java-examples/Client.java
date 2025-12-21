// Client.java
package helloworld;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Client {

    public static void main(String[] args) {
        // Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Create workflow stub
        GreetingWorkflow workflow = client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("greeting-workflow-1")
                        .setTaskQueue("greeting-queue")
                        .build()
        );

        // Execute workflow (blocking)
        String result = workflow.greet("Alice");

        System.out.println("Result: " + result);
    }
}