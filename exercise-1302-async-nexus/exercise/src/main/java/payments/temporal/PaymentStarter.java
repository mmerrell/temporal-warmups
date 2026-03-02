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
 * [GIVEN] Starts 3 payment workflows in parallel and waits for all results.
 *
 * Transactions:
 *   TXN-ALPHA: $3,500    US → Germany     ~15s investigation  APPROVED / LOW
 *   TXN-BETA:  $47,500   US → Caymans     ~35s investigation  APPROVED / MEDIUM
 *   TXN-GAMMA: $180,000  US → Iran        ~60s investigation  BLOCKED / CRITICAL
 *
 * Business ID workflow IDs:
 *   "payment-TXN-ALPHA", "payment-TXN-BETA", "payment-TXN-GAMMA"
 *
 * Runs workflows in parallel using WorkflowClient.execute().
 * All 3 investigations start at the same time.
 * Total time ≈ longest investigation (~60s for TXN-GAMMA) rather than 110s sequential.
 */
public class PaymentStarter {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   PAYMENT STARTER — Exercise 1302: Async Nexus          ║");
        System.out.println("║   Starting 3 transactions (parallel investigations)     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        PaymentRequest[] transactions = {
            new PaymentRequest("TXN-ALPHA", 3500.00, "USD", "US", "Germany",
                    "Equipment purchase from European supplier", "ACC-001", "ACC-002"),
            new PaymentRequest("TXN-BETA", 47500.00, "USD", "US", "Cayman Islands",
                    "Investment fund transfer to offshore account", "ACC-003", "ACC-004"),
            new PaymentRequest("TXN-GAMMA", 180000.00, "USD", "US", "Iran",
                    "Technology export to Middle East partner", "ACC-005", "ACC-006"),
        };

        List<CompletableFuture<PaymentResult>> futures = new ArrayList<>();

        for (PaymentRequest txn : transactions) {
            String workflowId = "payment-" + txn.getTransactionId();
            System.out.println("  Launching: " + workflowId
                    + " ($" + String.format("%.0f", txn.getAmount()) + " -> " + txn.getReceiverCountry() + ")");

            PaymentProcessingWorkflow workflow = client.newWorkflowStub(
                    PaymentProcessingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(Shared.TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .build());

            // WorkflowClient.execute() returns immediately — workflow runs in background
            CompletableFuture<PaymentResult> future =
                    WorkflowClient.execute(workflow::processPayment, txn);
            futures.add(future);
        }

        System.out.println("\n  All 3 workflows started. Waiting for results...");
        System.out.println("  (Investigations run in parallel — check Temporal UI!)\n");

        // Wait for all workflows to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    PAYMENT RESULTS                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        for (int i = 0; i < futures.size(); i++) {
            PaymentResult result = futures.get(i).get();
            String icon = "COMPLETED".equals(result.getStatus()) ? "✓" :
                         "BLOCKED_COMPLIANCE".equals(result.getStatus()) ? "✗" : "!";
            System.out.printf("  [%s] %-10s | %-20s | Risk: %-8s | %s%n",
                    icon,
                    result.getTransactionId(),
                    result.getStatus(),
                    result.getRiskLevel() != null ? result.getRiskLevel() : "N/A",
                    result.getSummary() != null ? result.getSummary().substring(0, Math.min(60, result.getSummary().length())) + "..." : "");
        }
        System.out.println();
    }
}
