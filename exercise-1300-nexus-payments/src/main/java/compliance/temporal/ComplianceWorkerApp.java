package compliance.temporal;

import compliance.FraudDetectionAgent;
import compliance.temporal.activity.FraudDetectionActivityImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * [STUDENT IMPLEMENTS] Compliance Team's Worker
 *
 * This worker runs on the Compliance team's infrastructure.
 * It handles:
 * 1. Nexus service requests (from Payments team via Nexus)
 * 2. FraudDetectionWorkflow execution
 * 3. FraudDetectionActivity execution
 *
 * KEY NEW CONCEPT: registerNexusServiceImplementation()
 * This is how you tell Temporal "this worker handles Nexus requests"
 *
 * Task queue: "compliance-risk" (matches the Nexus endpoint target)
 */
public class ComplianceWorkerApp {

    private static final String TASK_QUEUE = "compliance-risk";

    public static void main(String[] args) {
        // 1. Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // 3. Register workflows
        worker.registerWorkflowImplementationTypes(FraudDetectionWorkflowImpl.class);

        // 4. Register activities with business logic (DI pattern)
        FraudDetectionAgent fraudAgent = new FraudDetectionAgent();
        worker.registerActivitiesImplementations(new FraudDetectionActivityImpl(fraudAgent));

        // 5. KEY NEXUS CONCEPT: Register the Nexus service handler
        // This tells the worker to handle incoming Nexus operation requests
        worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl());

        // 6. Start
        factory.start();
        System.out.println("==========================================================");
        System.out.println("  Compliance Worker started");
        System.out.println("  Task Queue: " + TASK_QUEUE);
        System.out.println("  Handles: FraudDetectionWorkflow, Nexus operations");
        System.out.println("==========================================================");
        System.out.println("Waiting for Nexus requests from Payments team...\n");
    }
}
