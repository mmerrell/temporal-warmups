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
 * ═══════════════════════════════════════════════════════════════════
 *  KEY NEW CONCEPT: Nexus endpoint mapping at worker registration
 * ═══════════════════════════════════════════════════════════════════
 *
 * PaymentProcessingWorkflowImpl uses a Nexus stub:
 *   Workflow.newNexusServiceStub(ComplianceNexusService.class, ...)
 *
 * But the workflow doesn't know WHERE the Compliance endpoint lives.
 * That's configured HERE — keeping workflows portable across environments.
 *
 *   worker.registerWorkflowImplementationTypes(
 *       WorkflowImplementationOptions.newBuilder()
 *           .setNexusServiceOptions(
 *               Collections.singletonMap(
 *                   "ComplianceNexusService",           // interface name (no package)
 *                   NexusServiceOptions.newBuilder()
 *                       .setEndpoint("compliance-endpoint")  // matches the CLI endpoint
 *                       .build()))
 *           .build(),
 *       PaymentProcessingWorkflowImpl.class);
 *
 * ANALOGY: Like injecting a base URL via environment variable —
 *   the workflow defines WHAT to call, the worker defines WHERE.
 *
 * ── CRAWL pattern ────────────────────────────────────────────────
 *
 *   C — Connect: WorkflowServiceStubs + WorkflowClient
 *   R — Register: PaymentProcessingWorkflowImpl WITH Nexus endpoint mapping (above)
 *   A — Activities: PaymentActivityImpl (needs a PaymentGateway instance)
 *   W — (Wire is done inside the registerWorkflowImplementationTypes call)
 *   L — Launch: factory.start()
 *
 * Task queue: "payments-processing"
 *
 * ── What to implement ────────────────────────────────────────────
 *
 *   1. Connect to Temporal
 *   2. Create factory + worker on "payments-processing"
 *   3. Register PaymentProcessingWorkflowImpl WITH NexusServiceOptions
 *      (map "ComplianceNexusService" → "compliance-endpoint")
 *   4. Register PaymentActivityImpl with a PaymentGateway
 *   5. Start factory and print a startup banner
 */
public class PaymentsWorkerApp {

    // TODO: Define TASK_QUEUE = "payments-processing"
    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        // TODO: C — Connect to Temporal
        // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        // WorkflowClient client = WorkflowClient.newInstance(service);

        // TODO: R — Register workflow WITH Nexus endpoint mapping
        // WorkerFactory factory = WorkerFactory.newInstance(client);
        // Worker worker = factory.newWorker(TASK_QUEUE);
        //
        // worker.registerWorkflowImplementationTypes(
        //     WorkflowImplementationOptions.newBuilder()
        //         .setNexusServiceOptions(
        //             Collections.singletonMap(
        //                 "ComplianceNexusService",
        //                 NexusServiceOptions.newBuilder()
        //                     .setEndpoint("compliance-endpoint")
        //                     .build()))
        //         .build(),
        //     PaymentProcessingWorkflowImpl.class);

        // TODO: A — Register activities
        // PaymentGateway gateway = new PaymentGateway();
        // worker.registerActivitiesImplementations(new PaymentActivityImpl(gateway));

        // TODO: L — Launch!
        // factory.start();

        // TODO: Print startup banner
        System.out.println("TODO: implement PaymentsWorkerApp");
    }
}
