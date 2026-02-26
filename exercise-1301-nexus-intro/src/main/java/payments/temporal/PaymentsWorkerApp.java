package payments.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import payments.PaymentGateway;
import payments.temporal.activity.PaymentActivityImpl;

import java.util.Collections;

/**
 * YOUR TURN: Start the Payments team's worker.
 *
 * This is the standard CRAWL worker pattern, with one new twist
 * when registering the workflow implementation.
 *
 * CRAWL pattern:
 *   C — Connect to Temporal
 *   R — Register the workflow WITH Nexus endpoint mapping  ← the twist
 *   A — Activities: register PaymentActivityImpl
 *   W — (Nexus wiring happens inside the registration call at step R)
 *   L — Launch
 *
 * The twist in step R:
 *   In previous exercises you registered workflow implementations directly.
 *   Here you wrap the registration in WorkflowImplementationOptions so you
 *   can attach a NexusServiceOptions map. This map tells Temporal:
 *   "When this workflow uses a ComplianceNexusService stub, route those calls
 *   to the endpoint named 'compliance-endpoint'."
 *
 *   The map key is the service interface name (just the class name, no package).
 *   The map value is a NexusServiceOptions with the endpoint name set.
 *
 * ANALOGY: Like setting compliance.api.url=https://... in application.properties.
 *   The workflow defines WHAT to call (ComplianceNexusService).
 *   The worker defines WHERE to find it (compliance-endpoint).
 *
 * Task queue: "payments-processing"
 *
 * What to implement:
 *   1. Connect to Temporal (C)
 *   2. Create a worker on "payments-processing" (R)
 *   3. Register PaymentProcessingWorkflowImpl using WorkflowImplementationOptions
 *      that map "ComplianceNexusService" to the endpoint "compliance-endpoint"
 *   4. Register PaymentActivityImpl with a PaymentGateway instance (A)
 *   5. Launch and print a startup banner (L)
 */
public class PaymentsWorkerApp {

    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        // TODO: C — Connect to Temporal

        // TODO: R — Create factory and worker, then register PaymentProcessingWorkflowImpl
        //           using WorkflowImplementationOptions with Nexus endpoint mapping

        // TODO: A — Register PaymentActivityImpl (inject a PaymentGateway)

        // TODO: L — Launch and print startup banner
        throw new UnsupportedOperationException("TODO: implement PaymentsWorkerApp");
    }
}
