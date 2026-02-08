package solution.temporal;

import exercise.agents.*;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import solution.temporal.activity.CodeQualityActivityImpl;
import solution.temporal.activity.SecurityQualityActivityImpl;
import solution.temporal.activity.TestQualityActivityImpl;
import solution.temporal.workflow.BatchPRReviewWorkflowImpl;

public class WorkerApp {

    public static void main(String[] args) {
        // 1. Connect to Temporal server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(Shared.TASK_QUEUE);

        // 3. Register workflow implementation
        // Q: Why register the IMPL class, not the interface?
        // A: Temporal needs to instantiate the class to run workflows.
        worker.registerWorkflowImplementationTypes(BatchPRReviewWorkflowImpl.class);

        // 4. create the agents (business logic)
        CodeQualityAgent codeQualityAgent = new CodeQualityAgent();
        TestQualityAgent testQualityAgent = new TestQualityAgent();
        SecurityAgent securityAgent = new SecurityAgent();

        //5. Tell worker the activities that it can run
        //register the Activities and pass in business logic
        //first time we are creating ActivityImpls
        worker.registerActivitiesImplementations(
                new CodeQualityActivityImpl(codeQualityAgent),
                new TestQualityActivityImpl(testQualityAgent),
                new SecurityQualityActivityImpl(securityAgent)
        );

        // 6. Start the worker (blocks forever, listening for tasks)
        factory.start();
    }
}
