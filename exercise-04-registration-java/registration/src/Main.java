import i`o.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
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