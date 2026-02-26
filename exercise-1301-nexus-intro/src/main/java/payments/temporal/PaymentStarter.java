package payments.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;

/**
 * YOUR TURN: Start 3 payment workflows sequentially.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  BUSINESS ID WORKFLOW IDs (from Exercise 06a)
 * ═══════════════════════════════════════════════════════════════════
 *
 * Don't use: "payment-" + UUID.randomUUID()   ← random, hard to find
 * Instead:   "payment-" + txn.getTransactionId()  → "payment-TXN-A"
 *
 * Benefits:
 *   - Find this workflow instantly in the Temporal UI by transaction ID
 *   - Idempotent: re-running the starter with the same ID is safe
 *   - Meaningful audit trail for operations and compliance teams
 *
 * ── The 3 test transactions ──────────────────────────────────────
 *
 *   TXN-A: $250,    US → US         (LOW risk  — approved, processed)
 *   TXN-B: $12,000, US → UK         (MEDIUM risk — approved with note)
 *   TXN-C: $75,000, US → North Korea (HIGH risk  — declined by compliance)
 *
 *   Watch what happens in the Temporal UI for each one!
 *
 * ── START pattern ────────────────────────────────────────────────
 *
 *   S — Service: Connect to Temporal
 *   T — Target: Build WorkflowOptions (task queue + business ID)
 *   A — Acquire: Create typed workflow stub
 *   R — Run: Call the workflow method (blocks until complete for each)
 *   T — Track: Print result
 *
 * NOTE: In this exercise we run transactions sequentially (one at a time).
 *       In Exercise 1300 you'll start 5 workflows in PARALLEL using
 *       WorkflowClient.execute() + CompletableFuture.
 *
 * ── What to implement ────────────────────────────────────────────
 *
 *   1. Connect to Temporal (WorkflowServiceStubs + WorkflowClient)
 *   2. Define 3 PaymentRequest objects (TXN-A, TXN-B, TXN-C as above)
 *   3. For each transaction:
 *      a. Build WorkflowOptions with task queue "payments-processing"
 *         and workflowId = "payment-" + txn.getTransactionId()
 *      b. Create a typed stub: client.newWorkflowStub(PaymentProcessingWorkflow.class, options)
 *      c. Call: PaymentResult result = stub.processPayment(txn)  ← blocks until done
 *      d. Print the result (status, riskLevel, explanation, confirmationNumber)
 */
public class PaymentStarter {

    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  PAYMENT STARTER — Exercise 1301: Nexus Intro");
        System.out.println("  Running 3 transactions through Temporal + Nexus");
        System.out.println("==========================================================\n");

        // TODO: S — Connect to Temporal
        // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        // WorkflowClient client = WorkflowClient.newInstance(service);

        // TODO: Define 3 test transactions
        // TXN-A: $250,    US → US,          "Routine supplier payment"
        // TXN-B: $12,000, US → UK,          "International consulting fee"
        // TXN-C: $75,000, US → North Korea, "Business consulting services"
        //
        // PaymentRequest[] transactions = {
        //     new PaymentRequest("TXN-A", 250.00, "USD", "US", "US",
        //         "Routine supplier payment", "ACC-001", "ACC-002"),
        //     new PaymentRequest("TXN-B", 12000.00, "USD", "US", "UK",
        //         "International consulting fee", "ACC-003", "ACC-004"),
        //     new PaymentRequest("TXN-C", 75000.00, "USD", "US", "North Korea",
        //         "Business consulting services", "ACC-005", "ACC-006"),
        // };

        // TODO: For each transaction:
        //   T — Target: WorkflowOptions with workflowId = "payment-" + txn.getTransactionId()
        //   A — Acquire: client.newWorkflowStub(PaymentProcessingWorkflow.class, options)
        //   R — Run: result = stub.processPayment(txn)
        //   T — Track: print result

        System.out.println("TODO: implement PaymentStarter");
        System.out.println("\nView in Temporal UI: http://localhost:8233");
    }
}
