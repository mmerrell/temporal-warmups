package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {
    //1. create a task queue
    private static final String TASK_QUEUE = "email-verification";

    //2. build out the main()
    public static void main(String[] args) {
        //1. connect to temporal - create a service and client
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        //2. create a worker - factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // 3. Tell the worker what workflows it can execute
        worker.registerWorkflowImplementationTypes(EmailVerificationWorkflowImpl.class);

        // 3. Create business logic instances (dependency injection!)
        TokenGenerator tokenGenerator = new TokenGenerator();
        EmailVerifier emailSender = new EmailVerifier();

        // 4. Tell the worker what activities it can execute by registering
        // pass in the business logic objects
        worker.registerActivitiesImplementations(new EmailSendingActivityImpl(emailSender),
        new TokenGenerationActivityImpl(tokenGenerator));

        // 5. Start the worker - it runs forever, polling for tasks
        factory.start();

    }
}
