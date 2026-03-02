package compliance.temporal;

import compliance.ComplianceInvestigator;
import compliance.temporal.activity.ComplianceInvestigationActivityImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  TODO FILE 6 of 7: ComplianceWorkerApp — CRAWL Extended (15 min)
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: Register the full async compliance infrastructure.
 * Unlike 1301's compliance worker (Nexus handler only), this worker
 * ALSO registers the investigation workflow and activity.
 *
 * WHY THE DIFFERENCE?
 *   In 1301: The compliance logic ran INLINE inside the handler.
 *            Only the Nexus handler needed registration.
 *
 *   In 1302: The compliance logic runs as a REAL WORKFLOW.
 *            The handler just starts the workflow.
 *            The actual work (3 phases) happens in ComplianceInvestigationWorkflowImpl,
 *            which calls ComplianceInvestigationActivityImpl.
 *            So this worker must register ALL THREE.
 *
 * CRAWL PATTERN — Extended:
 *
 *   C — Connect:
 *       WorkflowServiceStubs.newLocalServiceStubs()
 *       WorkflowClient.newInstance(service)
 *
 *   R — Register workflow:   ← NEW vs 1301
 *       worker.registerWorkflowImplementationTypes(ComplianceInvestigationWorkflowImpl.class)
 *
 *   A — Register activities: ← NEW vs 1301
 *       worker.registerActivitiesImplementations(
 *           new ComplianceInvestigationActivityImpl(new ComplianceInvestigator()))
 *
 *   W — Wire Nexus handler:
 *       worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl())
 *
 *   L — Launch:
 *       factory.start()
 *
 * TASK QUEUE: "compliance-investigation"
 *   This MUST match --target-task-queue when you created the Nexus endpoint:
 *     temporal operator nexus endpoint create \
 *       --name compliance-endpoint \
 *       --target-namespace default \
 *       --target-task-queue compliance-investigation
 *
 *   If you used a different queue for the Nexus endpoint, Nexus calls will never
 *   reach this worker. The string must be identical.
 *
 * STARTUP BANNER:
 *   After factory.start(), print a banner like:
 *     System.out.println("Compliance Investigation Worker started on compliance-investigation");
 *     System.out.println("Waiting for Nexus requests from Payments team...");
 *
 * WHAT TO IMPLEMENT:
 *   Follow the CRAWL steps above, in order.
 */
public class ComplianceWorkerApp {

    static final String TASK_QUEUE = "compliance-investigation";

    public static void main(String[] args) {
        // TODO: C — Connect to Temporal
        // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        // WorkflowClient client = WorkflowClient.newInstance(service);

        // TODO: Create WorkerFactory and Worker on TASK_QUEUE ("compliance-investigation")
        // WorkerFactory factory = WorkerFactory.newInstance(client);
        // Worker worker = factory.newWorker(TASK_QUEUE);

        // TODO: R — Register ComplianceInvestigationWorkflowImpl
        // worker.registerWorkflowImplementationTypes(ComplianceInvestigationWorkflowImpl.class);

        // TODO: A — Register ComplianceInvestigationActivityImpl
        //           Inject a new ComplianceInvestigator
        // worker.registerActivitiesImplementations(
        //     new ComplianceInvestigationActivityImpl(/* new ComplianceInvestigator() */ ));

        // TODO: W — Register the Nexus handler
        // worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl());

        // TODO: L — Start the factory
        // factory.start();

        // TODO: Print startup banner
        System.out.println("ComplianceWorkerApp: not implemented yet.");
        System.out.println("Implement the TODO steps above.");
    }
}
