package payments.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import payments.Shared;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * [GIVEN] Same as PaymentStarter but uses timestamp-suffixed workflow IDs.
 * Used for Checkpoint 4 (The Heist Test) — allows you to re-run without
 * ALREADY_EXISTS errors.
 *
 * Usage: mvn compile exec:java@starter-rerun
 */
public class PaymentStarterRerun {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   PAYMENT STARTER RERUN — Exercise 1302: Heist Test     ║");
        System.out.println("║   Starting 3 transactions (timestamp-suffixed IDs)      ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        long ts = System.currentTimeMillis();

        PaymentRequest[] transactions = {
            new PaymentRequest("TXN-ALPHA-" + ts, 3500.00, "USD", "US", "Germany",
                    "Equipment purchase from European supplier", "ACC-001", "ACC-002"),
            new PaymentRequest("TXN-BETA-" + ts, 47500.00, "USD", "US", "Cayman Islands",
                    "Investment fund transfer to offshore account", "ACC-003", "ACC-004"),
            new PaymentRequest("TXN-GAMMA-" + ts, 180000.00, "USD", "US", "Iran",
                    "Technology export to Middle East partner", "ACC-005", "ACC-006"),
        };

        List<CompletableFuture<PaymentResult>> futures = new ArrayList<>();

        for (PaymentRequest txn : transactions) {
            String workflowId = "payment-" + txn.getTransactionId();
            System.out.println("  Launching: " + workflowId);

            PaymentProcessingWorkflow workflow = client.newWorkflowStub(
                    PaymentProcessingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(Shared.TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .build());

            CompletableFuture<PaymentResult> future =
                    WorkflowClient.execute(workflow::processPayment, txn);
            futures.add(future);
        }

        System.out.println("\n  All 3 workflows started. Kill the compliance worker during TXN-BETA!");
        System.out.println("  Watch it resume from Phase 3 after restart.\n");

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("\n═══ RESULTS ═══");
        for (int i = 0; i < futures.size(); i++) {
            PaymentResult result = futures.get(i).get();
            System.out.printf("  %-25s | %-20s | Risk: %s%n",
                    result.getTransactionId(),
                    result.getStatus(),
                    result.getRiskLevel() != null ? result.getRiskLevel() : "N/A");
        }
    }
}
