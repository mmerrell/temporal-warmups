package payments.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import payments.PaymentGateway;
import payments.Shared;
import payments.temporal.activity.PaymentActivityImpl;

import java.util.Collections;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  TODO FILE 7 of 7: PaymentsWorkerApp — CRAWL Familiar (10 min)
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: Wire the payments side. Same CRAWL pattern as Exercise 1301.
 * Repetition reinforces the pattern.
 *
 * CRAWL PATTERN:
 *
 *   C — Connect:
 *       WorkflowServiceStubs.newLocalServiceStubs()
 *       WorkflowClient.newInstance(service)
 *
 *   R — Register workflow WITH Nexus endpoint mapping:
 *       WorkflowImplementationOptions maps service name → endpoint name.
 *       This is identical to 1301 — the workflow defines WHAT to call,
 *       the worker defines WHERE to find it.
 *
 *       worker.registerWorkflowImplementationTypes(
 *           WorkflowImplementationOptions.newBuilder()
 *               .setNexusServiceOptions(Collections.singletonMap(
 *                   "ComplianceNexusService",
 *                   NexusServiceOptions.newBuilder()
 *                       .setEndpoint("compliance-endpoint")
 *                       .build()))
 *               .build(),
 *           PaymentProcessingWorkflowImpl.class);
 *
 *   A — Register activities:
 *       worker.registerActivitiesImplementations(new PaymentActivityImpl(new PaymentGateway()))
 *
 *   W — (endpoint mapping done in R step above — no separate W step needed)
 *
 *   L — Launch and print startup banner:
 *       factory.start()
 *       Print: "Payments Worker started on payments-processing"
 *
 * TASK QUEUE: Shared.TASK_QUEUE ("payments-processing")
 *
 * REMINDER:
 *   The "ComplianceNexusService" string is the class name (no package).
 *   The "compliance-endpoint" string must match --name from the CLI command:
 *     temporal operator nexus endpoint create --name compliance-endpoint ...
 *
 * WHAT TO IMPLEMENT:
 *   Follow the CRAWL steps above. All necessary imports are already present.
 */
public class PaymentsWorkerApp {

    public static void main(String[] args) {
        // TODO: C — Connect to Temporal
        // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        // WorkflowClient client = WorkflowClient.newInstance(service);

        // TODO: Create WorkerFactory and Worker on Shared.TASK_QUEUE
        // WorkerFactory factory = WorkerFactory.newInstance(client);
        // Worker worker = factory.newWorker(Shared.TASK_QUEUE);

        // TODO: R — Register PaymentProcessingWorkflowImpl with Nexus endpoint mapping
        // Map "ComplianceNexusService" → "compliance-endpoint"
        // Use WorkflowImplementationOptions.newBuilder().setNexusServiceOptions(...)

        // TODO: A — Register PaymentActivityImpl with a new PaymentGateway
        // worker.registerActivitiesImplementations(new PaymentActivityImpl(/* new PaymentGateway() */ ));

        // TODO: L — Launch and print startup banner
        // factory.start();

        System.out.println("PaymentsWorkerApp: not implemented yet.");
        System.out.println("Implement the TODO steps above.");
    }
}
