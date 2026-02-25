package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {
    public static void main(String[] args) {
        // 1. Connect to Temporal server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(SharedData.TASK_QUEUE);

        // 3. Register workflow implementation
        // Q: Why register the IMPL class, not the interface?
        // A: Temporal needs to instantiate the class to run workflows.
        worker.registerWorkflowImplementationTypes(OrderLifecycleWorkflowImpl.class);

        //4. Tell worker the activities that it can run
        //register the Activities and pass in business logic
        //first time we are creating ActivityImpls
        worker.registerActivitiesImplementations(
                new OrderActivitiesImpl()
        );

        // 5. Start the worker (blocks forever, listening for tasks)
        factory.start();

    }
}
