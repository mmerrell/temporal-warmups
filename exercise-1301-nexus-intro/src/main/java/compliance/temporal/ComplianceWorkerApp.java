package compliance.temporal;

import compliance.ComplianceAgent;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * YOUR TURN: Start the Compliance team's worker.
 *
 * This is mostly the standard worker pattern from previous exercises,
 * but with ONE KEY ADDITION: registerNexusServiceImplementation().
 *
 * ═══════════════════════════════════════════════════════════════════
 *  KEY NEW CONCEPT: registerNexusServiceImplementation()
 * ═══════════════════════════════════════════════════════════════════
 *
 *   Without this call, the worker ignores all incoming Nexus requests.
 *   With it, Temporal routes Nexus calls to your @ServiceImpl handler.
 *
 *     ComplianceAgent agent = new ComplianceAgent();
 *     worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(agent));
 *
 *   Compare to registering activities:
 *     worker.registerActivitiesImplementations(new MyActivityImpl());  // familiar
 *     worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(...)); // new!
 *
 * ── CRAWL pattern (Workers CRAWL before they run) ────────────────
 *
 *   C — Connect to Temporal (WorkflowServiceStubs + WorkflowClient)
 *   R — Register workflow types (none in this worker — Compliance has no workflows in 1301)
 *   A — Activities (none — compliance logic is in the Nexus handler, not activities)
 *   W — Wire Nexus service handler (registerNexusServiceImplementation)
 *   L — Launch! (factory.start())
 *
 * Task queue: "compliance-risk"
 *   (must match the Nexus endpoint target — see the CLI command in the README)
 *
 * ── In Exercise 1300, this worker also ──────────────────────────
 *   - Registers FraudDetectionWorkflow (an async Nexus handler starts a workflow)
 *   - Registers FraudDetectionActivity (the LLM call wrapped as an activity)
 *
 * ── What to implement ────────────────────────────────────────────
 *
 *   1. Connect to Temporal (WorkflowServiceStubs, WorkflowClient, WorkerFactory)
 *   2. Create a worker on TASK_QUEUE = "compliance-risk"
 *   3. Create a ComplianceAgent instance
 *   4. Call worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(agent))
 *   5. Call factory.start()
 *   6. Print a startup banner
 */
public class ComplianceWorkerApp {

    // TODO: Define TASK_QUEUE = "compliance-risk"
    private static final String TASK_QUEUE = "compliance-risk";

    public static void main(String[] args) {
        // TODO: C — Connect to Temporal
        // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        // WorkflowClient client = WorkflowClient.newInstance(service);

        // TODO: R — Register (none for 1301, but set up the worker)
        // WorkerFactory factory = WorkerFactory.newInstance(client);
        // Worker worker = factory.newWorker(TASK_QUEUE);

        // TODO: W — Wire Nexus service handler
        // ComplianceAgent agent = new ComplianceAgent();
        // worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(agent));

        // TODO: L — Launch!
        // factory.start();

        // TODO: Print startup banner
        System.out.println("TODO: implement ComplianceWorkerApp");
    }
}
