package compliance.temporal;

import compliance.ComplianceAgent;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * YOUR TURN: Start the Compliance team's worker.
 *
 * This is the standard CRAWL worker pattern from previous exercises,
 * with ONE new step: registering the Nexus service implementation.
 *
 * CRAWL pattern — Workers CRAWL before they run:
 *   C — Connect to Temporal
 *   R — Register (no workflows in this worker for exercise 1301)
 *   A — Activities (none — compliance logic lives in the Nexus handler)
 *   W — Wire the Nexus service handler  ← NEW
 *   L — Launch
 *
 * The key new method is registerNexusServiceImplementation().
 * Without it, the worker silently ignores all incoming Nexus requests.
 * With it, Temporal routes calls to your @ServiceImpl handler.
 *
 * Compare to what you already know:
 *   Registering activities: worker.registerActivitiesImplementations(...)
 *   Registering Nexus:      worker.registerNexusServiceImplementation(...)
 *   Same shape, different method name.
 *
 * Task queue must be "compliance-risk" — this is what you set as
 * --target-task-queue when creating the Nexus endpoint via the CLI.
 * If these strings don't match, Nexus calls will never reach this worker.
 *
 * In Exercise 1300, this worker also registers a FraudDetectionWorkflow
 * and a FraudDetectionActivity for the async Nexus handler.
 *
 * What to implement:
 *   1. Connect to Temporal (C)
 *   2. Create a worker on task queue "compliance-risk" (R)
 *   3. Create a ComplianceAgent instance
 *   4. Register the Nexus service implementation (W)
 *   5. Launch the factory (L)
 *   6. Print a startup banner so you know it's running
 */
public class ComplianceWorkerApp {

    private static final String TASK_QUEUE = "compliance-risk";

    public static void main(String[] args) {
        // TODO: C — Connect to Temporal

        // TODO: R — Create the worker factory and worker on TASK_QUEUE

        // TODO: W — Create a ComplianceAgent and register the Nexus service implementation

        // TODO: L — Start the factory

        // TODO: Print startup banner
        throw new UnsupportedOperationException("TODO: implement ComplianceWorkerApp");
    }
}
