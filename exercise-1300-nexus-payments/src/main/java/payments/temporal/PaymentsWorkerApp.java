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
 * YOUR TURN: Implement the Payments team's worker.
 *
 * This is mostly the standard worker pattern from previous exercises,
 * but with ONE KEY DIFFERENCE: Nexus endpoint mapping.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  KEY CONCEPT: Workflow registration WITH Nexus endpoint mapping
 * ═══════════════════════════════════════════════════════════════════
 *
 * The PaymentProcessingWorkflow uses a Nexus service stub:
 *   Workflow.newNexusServiceStub(ComplianceNexusService.class, ...)
 *
 * But the workflow doesn't know WHERE the Compliance endpoint lives.
 * That's configured HERE in the worker, keeping workflows portable:
 *
 *   worker.registerWorkflowImplementationTypes(
 *       WorkflowImplementationOptions.newBuilder()
 *           .setNexusServiceOptions(
 *               Collections.singletonMap(
 *                   "ComplianceNexusService",     // Service interface name
 *                   NexusServiceOptions.newBuilder()
 *                       .setEndpoint("compliance-endpoint")  // Matches CLI endpoint
 *                       .build()))
 *           .build(),
 *       PaymentProcessingWorkflowImpl.class);
 *
 * ANALOGY: Like a Spring @Value("${compliance.url}") — the workflow
 *          defines WHAT it calls, the worker config defines WHERE.
 *
 * Steps:
 *   1. Connect to Temporal (WorkflowServiceStubs + WorkflowClient)
 *   2. Create a WorkerFactory and Worker on task queue "payments-processing"
 *   3. Register PaymentProcessingWorkflowImpl WITH Nexus options (see above)
 *   4. Register PaymentActivityImpl with a PaymentGateway instance
 *   5. Start the factory
 *
 * Task queue: "payments-processing"
 */
public class PaymentsWorkerApp {

    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        // TODO: 1. Connect to Temporal
        //   WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        //   WorkflowClient client = WorkflowClient.newInstance(service);

        // TODO: 2. Create worker factory and worker
        //   WorkerFactory factory = WorkerFactory.newInstance(client);
        //   Worker worker = factory.newWorker(TASK_QUEUE);

        // TODO: 3. Register workflow WITH Nexus endpoint mapping
        //   worker.registerWorkflowImplementationTypes(
        //       WorkflowImplementationOptions.newBuilder()
        //           .setNexusServiceOptions(
        //               Collections.singletonMap(
        //                   "ComplianceNexusService",
        //                   NexusServiceOptions.newBuilder()
        //                       .setEndpoint("compliance-endpoint")
        //                       .build()))
        //           .build(),
        //       PaymentProcessingWorkflowImpl.class);

        // TODO: 4. Register activities with business logic (DI pattern)
        //   PaymentGateway gateway = new PaymentGateway();
        //   worker.registerActivitiesImplementations(new PaymentActivityImpl(gateway));

        // TODO: 5. Start the factory
        //   factory.start();

        System.out.println("==========================================================");
        System.out.println("  Payments Worker started");
        System.out.println("  Task Queue: " + TASK_QUEUE);
        System.out.println("  Nexus Endpoint: compliance-endpoint -> ComplianceNexusService");
        System.out.println("  Handles: PaymentProcessingWorkflow");
        System.out.println("==========================================================");
        System.out.println("Waiting for payment requests...\n");
    }
}
