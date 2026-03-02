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
 * [SOLUTION] Payments team's worker — identical CRAWL pattern to Exercise 1301.
 *
 * CRAWL:
 *   C — Connect
 *   R — Register PaymentProcessingWorkflowImpl with Nexus endpoint mapping
 *   A — Register PaymentActivityImpl
 *   W — (done in R step via WorkflowImplementationOptions)
 *   L — Launch
 *
 * The endpoint mapping ("ComplianceNexusService" → "compliance-endpoint")
 * tells Temporal where to route Nexus calls from this workflow.
 */
public class PaymentsWorkerApp {

    public static void main(String[] args) {
        // C — Connect
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(Shared.TASK_QUEUE);

        // R + W — Register workflow with Nexus endpoint mapping
        worker.registerWorkflowImplementationTypes(
                WorkflowImplementationOptions.newBuilder()
                        .setNexusServiceOptions(Collections.singletonMap(
                                "ComplianceNexusService",
                                NexusServiceOptions.newBuilder()
                                        .setEndpoint("compliance-endpoint")
                                        .build()))
                        .build(),
                PaymentProcessingWorkflowImpl.class);

        // A — Register payment activity
        worker.registerActivitiesImplementations(new PaymentActivityImpl(new PaymentGateway()));

        // L — Launch
        factory.start();

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   Payments Worker — ONLINE                               ║");
        System.out.println("║   Task queue: payments-processing                        ║");
        System.out.println("║   Nexus endpoint: compliance-endpoint                    ║");
        System.out.println("║   Ready to process payment workflows...                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
