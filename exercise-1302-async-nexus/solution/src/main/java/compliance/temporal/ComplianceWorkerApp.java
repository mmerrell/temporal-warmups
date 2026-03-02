package compliance.temporal;

import compliance.ComplianceInvestigator;
import compliance.temporal.activity.ComplianceInvestigationActivityImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * [SOLUTION] Compliance team's worker — registers workflow, activity, and Nexus handler.
 *
 * CRAWL pattern extended (vs Exercise 1301's handler-only worker):
 *   C — Connect
 *   R — Register ComplianceInvestigationWorkflowImpl   ← NEW
 *   A — Register ComplianceInvestigationActivityImpl   ← NEW
 *   W — Wire Nexus handler
 *   L — Launch
 *
 * Task queue: "compliance-investigation"
 * Must match --target-task-queue in the CLI endpoint creation command.
 */
public class ComplianceWorkerApp {

    static final String TASK_QUEUE = "compliance-investigation";

    public static void main(String[] args) {
        // C — Connect
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // R — Register investigation workflow
        worker.registerWorkflowImplementationTypes(ComplianceInvestigationWorkflowImpl.class);

        // A — Register investigation activity
        worker.registerActivitiesImplementations(
                new ComplianceInvestigationActivityImpl(new ComplianceInvestigator()));

        // W — Register Nexus handler
        worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl());

        // L — Launch
        factory.start();

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   Compliance Investigation Worker — ONLINE               ║");
        System.out.println("║   Task queue: compliance-investigation                   ║");
        System.out.println("║   Registered: investigation workflow + activity + Nexus  ║");
        System.out.println("║   Waiting for payment investigation requests...          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
