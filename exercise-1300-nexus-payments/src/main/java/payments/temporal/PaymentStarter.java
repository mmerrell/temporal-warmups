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
 * YOUR TURN: Start all 5 payment workflows in parallel.
 *
 * This combines two patterns from Exercise 06a:
 *
 * ═══════════════════════════════════════════════════════════════════
 *  PARALLEL EXECUTION (Exercise 06a pattern)
 * ═══════════════════════════════════════════════════════════════════
 *
 *   WorkflowClient.start(wf::method, input)   → Fire and forget
 *   WorkflowClient.execute(wf::method, input)  → Returns CompletableFuture<R>
 *
 *   Use execute() to start all workflows, collect futures, then wait:
 *
 *     List<CompletableFuture<PaymentResult>> futures = new ArrayList<>();
 *     for (PaymentRequest txn : transactions) {
 *         PaymentProcessingWorkflow wf = client.newWorkflowStub(
 *             PaymentProcessingWorkflow.class, options);
 *         futures.add(WorkflowClient.execute(wf::processPayment, txn));
 *     }
 *     // Wait for ALL to complete (high-risk ones block until signaled!)
 *     for (CompletableFuture<PaymentResult> f : futures) {
 *         PaymentResult result = f.get();  // blocks until this workflow completes
 *     }
 *
 * ═══════════════════════════════════════════════════════════════════
 *  BUSINESS ID WORKFLOW IDs (Exercise 06a pattern)
 * ═══════════════════════════════════════════════════════════════════
 *
 *   Don't use: "payment-" + UUID.randomUUID()
 *   Instead:   "payment-" + txn.getTransactionId()  → "payment-TXN-001"
 *
 *   Benefits: findable in Temporal UI, idempotent, meaningful for debugging
 *
 * ═══════════════════════════════════════════════════════════════════
 *
 * Steps (START pattern — "Starters START workflows"):
 *   S — Service: Connect to Temporal (WorkflowServiceStubs + WorkflowClient)
 *   T — Target: Build WorkflowOptions (task queue + business ID like "payment-TXN-001")
 *   A — Acquire: Create workflow stub (client.newWorkflowStub)
 *   R — Run: Fire off workflows (WorkflowClient.execute → CompletableFuture)
 *   T — Track: Wait for results (future.get() — high-risk ones block until signaled!)
 */
public class PaymentStarter {

    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  PAYMENT STARTER - Starting 5 payments via Temporal");
        System.out.println("==========================================================\n");

        // Same 5 transactions as the pre-Temporal baseline
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

        // S — Service: Connect to Temporal
        // TODO:
        //   WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        //   WorkflowClient client = WorkflowClient.newInstance(service);

        // T, A, R — Target + Acquire + Run (in a loop for parallel execution)
        // TODO:
        //   List<CompletableFuture<PaymentResult>> futures = new ArrayList<>();
        //   List<String> workflowIds = new ArrayList<>();
        //
        //   for (PaymentRequest txn : transactions) {
        //       // T — Target: business ID workflow ID
        //       String workflowId = "payment-" + txn.getTransactionId();
        //       workflowIds.add(workflowId);
        //
        //       // A — Acquire: typed workflow stub
        //       PaymentProcessingWorkflow workflow = client.newWorkflowStub(
        //           PaymentProcessingWorkflow.class,
        //           WorkflowOptions.newBuilder()
        //               .setTaskQueue(TASK_QUEUE)
        //               .setWorkflowId(workflowId)
        //               .build());
        //
        //       // R — Run: fire off workflow (returns CompletableFuture)
        //       System.out.println("  Starting: " + workflowId);
        //       futures.add(WorkflowClient.execute(workflow::processPayment, txn));
        //   }

        // T — Track: Wait for results
        // TODO:
        //   System.out.println("\nAll workflows started! Waiting for results...");
        //   System.out.println("High-risk transactions need approval signals.\n");
        //
        //   for (int i = 0; i < futures.size(); i++) {
        //       try {
        //           PaymentResult result = futures.get(i).get();
        //           System.out.println("  " + workflowIds.get(i) + " → "
        //               + result.getStatus()
        //               + (result.getRiskLevel() != null ? " | Risk: " + result.getRiskLevel() : "")
        //               + (result.getConfirmationNumber() != null ? " | Conf: " + result.getConfirmationNumber() : ""));
        //       } catch (InterruptedException | ExecutionException e) {
        //           System.err.println("  " + workflowIds.get(i) + " → ERROR: " + e.getMessage());
        //       }
        //   }

        System.out.println("\n==========================================================");
        System.out.println("  View in Temporal UI: http://localhost:8233");
        System.out.println("  View in Dashboard:   http://localhost:3000");
        System.out.println("==========================================================");
    }
}
