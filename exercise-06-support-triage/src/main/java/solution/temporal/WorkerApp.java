package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import solution.temporal.activity.PIIScrubberActivityImpl;
import solution.temporal.activity.TicketClassifierActivityImpl;

public class WorkerApp {
    //1. task queue same as Starter
    private static final String TASK_QUEUE = "support-triage";

    //2. main() just like Starter
    public static void main(String[] args) {
        //3. connect to temporal with a service and client
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        //4. create a factory and worker - unique to Worker of course
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        //5. register the workflow Impl it can run
        worker.registerWorkflowImplementationTypes(SupportTriageWorkflowImpl.class);

        //6. Create Business Logic instances for DI - these will not exist
        // we will use the IDE to auto populate
        PIIScrubber piiScrubber = new PIIScrubber();
        TicketClassifier ticketClassifier = new TicketClassifier();

        //7. Tell worker the activities that it can run
        //register the Activities and pass in business logic
        //first time we are creating ActivityImpls
        worker.registerActivitiesImplementations(new PIIScrubberActivityImpl(piiScrubber),
                new TicketClassifierActivityImpl(ticketClassifier));

        factory.start();
    }
}
