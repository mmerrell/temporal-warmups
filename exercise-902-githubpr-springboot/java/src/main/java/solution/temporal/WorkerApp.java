package solution.temporal;

import exercise.agents.CodeQualityAgent;
import exercise.agents.SecurityAgent;
import exercise.agents.TestQualityAgent;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {
    //1. create a task queue
    private static String TASK_QUEUE = "pr-review";

    //2. create main()
    public static void main(String[] args){
        //3. connect to temporal with a service and client
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        //4. create a factory and worker - unique to Worker of course
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        //5. register the WorkflowImpl
        worker.registerWorkflowImplementationTypes(PRReviewWorkflowImpl.class);

        //6. Create Business Logic instances for DI - these will not exist
        // we will use the IDE to auto populate
        CodeQualityAgent codeQualityAgent = new CodeQualityAgent();
        TestQualityAgent testQualityAgent = new TestQualityAgent();
        SecurityAgent securityAgent = new SecurityAgent();

        //7. Tell worker the activities that it can run
        //register the Activities and pass in business logic
        //first time we are creating ActivityImpls
        worker.registerActivitiesImplementations(
                new CodeQualityActivityImpl(codeQualityAgent),
                new TestQualityActivityImpl(testQualityAgent),
                new SecurityAgentActivityImpl(securityAgent)
        );

        //8. start the factory
        factory.start();
    }
}
