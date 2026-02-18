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
 * [STUDENT IMPLEMENTS] Payments Team's Worker
 *
 * This worker runs on the Payments team's infrastructure.
 * It handles:
 * 1. PaymentProcessingWorkflow execution
 * 2. PaymentActivity execution
 *
 * KEY NEW CONCEPT: Workflow registration with Nexus service options
 * The worker needs to know where Nexus endpoints are so workflows
 * can call cross-team services.
 *
 * Task queue: "payments-processing"
 */
public class PaymentsWorkerApp {

    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        // 1. Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // 3. KEY NEXUS CONCEPT: Register workflow WITH Nexus endpoint mapping
        // This tells the workflow where to find the Compliance team's Nexus endpoint.
        // "ComplianceNexusService" (the service name) maps to "compliance-endpoint" (the CLI endpoint).
        worker.registerWorkflowImplementationTypes(
                WorkflowImplementationOptions.newBuilder()
                        .setNexusServiceOptions(
                                Collections.singletonMap(
                                        "ComplianceNexusService",
                                        NexusServiceOptions.newBuilder()
                                                .setEndpoint("compliance-endpoint")
                                                .build()))
                        .build(),
                PaymentProcessingWorkflowImpl.class);

        // 4. Register activities with business logic (DI pattern)
        PaymentGateway gateway = new PaymentGateway();
        worker.registerActivitiesImplementations(new PaymentActivityImpl(gateway));

        // 5. Start
        factory.start();
        System.out.println("==========================================================");
        System.out.println("  Payments Worker started");
        System.out.println("  Task Queue: " + TASK_QUEUE);
        System.out.println("  Nexus Endpoint: compliance-endpoint -> ComplianceNexusService");
        System.out.println("  Handles: PaymentProcessingWorkflow");
        System.out.println("==========================================================");
        System.out.println("Waiting for payment requests...\n");
    }
}
