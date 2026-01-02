package solution.temporal;

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

        // Create service instances (dependencies)
        UserValidator userValidator = new UserValidator();
        UserCreator userCreator = new UserCreator();
        EmailService emailService = new EmailService();


        //3. register workflow
        worker.registerWorkflowImplementationTypes(RegistrationWorkflowImpl.class);

        //4. register activities
        worker.registerActivitiesImplementations(
                new UserValidatorActivityImpl(userValidator),
                new UserRecordCreationActivityImpl(userCreator),
                new EmailActivityImpl(emailService),
                new VerificationEmailActivityImpl(emailService));

        //5. start
        factory.start();
    }
}
