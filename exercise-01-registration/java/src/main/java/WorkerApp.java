import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {
    private static final String TASK_QUEUE = "registration";

    public static void main(String[] args) {
        //1. connect to temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        //2. create worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        //3. register workflow
        worker.registerWorkflowImplementationTypes(RegistrationWorkflowImpl.class);

        //4. start
        factory.start();
    }
}
