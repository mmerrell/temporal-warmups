package payments.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * [STUDENT IMPLEMENTS] Starts 5 payment workflows in parallel.
 *
 * Uses patterns from Exercise 06a:
 * - Business identifier workflow IDs: "payment-TXN-001"
 * - Parallel execution: WorkflowClient.execute() returns CompletableFuture
 *
 * 5 transactions with realistic FinServ risk patterns:
 * TXN-001: $250 US->US (rent) - Low risk, auto-completes
 * TXN-002: $49,999 US->Cayman Islands (investment) - Suspicious destination
 * TXN-003: $12.50 US->US (coffee) - Low risk, auto-completes
 * TXN-004: $150,000 Russia->US (consulting) - Sanctions risk
 * TXN-005: $9,999 US->US (cash deposit) - Structuring? Just under $10k
 */
public class PaymentStarter {

    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 5 sample transactions
        PaymentRequest[] transactions = {
            new PaymentRequest("TXN-001", 250.00, "USD", "US", "US",
                    "Monthly rent payment", "ACC-SENDER-001", "ACC-RECV-001"),
            new PaymentRequest("TXN-002", 49999.00, "USD", "US", "Cayman Islands",
                    "Investment fund transfer", "ACC-SENDER-002", "ACC-RECV-002"),
            new PaymentRequest("TXN-003", 12.50, "USD", "US", "US",
                    "Coffee shop purchase", "ACC-SENDER-003", "ACC-RECV-003"),
            new PaymentRequest("TXN-004", 150000.00, "USD", "Russia", "US",
                    "Business consulting payment", "ACC-SENDER-004", "ACC-RECV-004"),
            new PaymentRequest("TXN-005", 9999.00, "USD", "US", "US",
                    "Cash deposit", "ACC-SENDER-005", "ACC-RECV-005"),
        };

        System.out.println("==========================================================");
        System.out.println("  PAYMENT STARTER - Launching 5 payments in parallel");
        System.out.println("==========================================================\n");

        long startTime = System.currentTimeMillis();
        List<CompletableFuture<PaymentResult>> futures = new ArrayList<>();
        List<String> workflowIds = new ArrayList<>();

        // Start all workflows in parallel (non-blocking)
        for (PaymentRequest txn : transactions) {
            // Business identifier workflow ID pattern (Ex 06a)
            String workflowId = "payment-" + txn.getTransactionId();
            workflowIds.add(workflowId);

            PaymentProcessingWorkflow workflow = client.newWorkflowStub(
                    PaymentProcessingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .build()
            );

            // Parallel execution pattern (Ex 06a)
            CompletableFuture<PaymentResult> future =
                    WorkflowClient.execute(workflow::processPayment, txn);
            futures.add(future);

            System.out.println("  Started: " + workflowId + " | $"
                    + String.format("%.2f", txn.getAmount())
                    + " | " + txn.getSenderCountry() + " -> " + txn.getReceiverCountry());
        }

        long startElapsed = System.currentTimeMillis() - startTime;
        System.out.println("\nAll " + transactions.length + " workflows started in " + startElapsed + "ms");
        System.out.println("\nNote: High-risk transactions will wait for approval signals.");
        System.out.println("Send approval: temporal workflow signal --workflow-id payment-TXN-002 \\");
        System.out.println("  --name approveTransaction --input '{\"approved\":true,\"reviewerName\":\"Jane\",\"reason\":\"Verified\"}'\n");
        System.out.println("Waiting for low-risk transactions to complete...\n");

        // Collect results as they complete
        for (int i = 0; i < futures.size(); i++) {
            try {
                PaymentResult result = futures.get(i).get();
                String status = result.isSuccess() ? "SUCCESS" : "WAITING/FAILED";
                System.out.println("  " + workflowIds.get(i) + " - " + status
                        + " | Status: " + result.getStatus()
                        + (result.getRiskLevel() != null ? " | Risk: " + result.getRiskLevel() : "")
                        + (result.getConfirmationNumber() != null ? " | Conf: " + result.getConfirmationNumber() : "")
                        + (result.getError() != null ? " | Error: " + result.getError() : ""));
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("  " + workflowIds.get(i) + " - ERROR: " + e.getMessage());
            }
        }

        long totalElapsed = System.currentTimeMillis() - startTime;

        System.out.println("\n==========================================================");
        System.out.println("  RESULTS");
        System.out.println("==========================================================");
        System.out.println("  Total time: " + totalElapsed + "ms");
        System.out.println("\n  View in Temporal UI: http://localhost:8233");
        System.out.println("  Search by workflow ID: payment-TXN-001 through payment-TXN-005");
        System.out.println("  See Nexus operations linking Payment and Compliance workflows!");
        System.out.println("==========================================================");
    }
}
