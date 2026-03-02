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
 * YOUR TURN: Start 3 payment workflows sequentially.
 *
 * WIRE pattern — to send a payment, you WIRE it:
 *   W — Wire:     Connect to Temporal (WorkflowServiceStubs + WorkflowClient)
 *   I — ID+queue: Set workflowId and taskQueue in WorkflowOptions
 *   R — Resolve:  Create a typed stub via client.newWorkflowStub()
 *   E — Execute:  Call the workflow method — blocks until the workflow completes
 *
 * The 3 test transactions to define:
 *   TXN-A: $250,     US → US,           "Routine supplier payment"       (LOW risk)
 *   TXN-B: $12,000,  US → UK,           "International consulting fee"   (MEDIUM risk)
 *   TXN-C: $75,000,  US → North Korea,  "Business consulting services"   (HIGH — declined)
 *
 * Use placeholder account numbers like "ACC-001" / "ACC-002" etc.
 *
 * Business ID pattern — use a meaningful workflow ID, not a random UUID:
 *   "payment-" + txn.getTransactionId()  →  "payment-TXN-A"
 *   Why: findable in the Temporal UI, idempotent, meaningful audit trail.
 *
 * Transactions run sequentially here — one completes before the next starts.
 * In Exercise 1300 you'll start 5 workflows in parallel with CompletableFuture.
 *
 * What to implement:
 *   1. W — Wire the connection to Temporal
 *   2. Define the 3 PaymentRequest objects above
 *   3. For each transaction (I, R, E):
 *      - I: Build WorkflowOptions with task queue "payments-processing"
 *           and a business ID workflow ID
 *      - R: Create a typed workflow stub from the client
 *      - E: Call processPayment() on the stub — this blocks until done
 *      - Print the result: status, riskLevel, explanation, confirmationNumber
 */
public class PaymentStarter {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  PAYMENT STARTER — Exercise 1301: Nexus Intro");
        System.out.println("  Running 3 transactions through Temporal + Nexus");
        System.out.println("==========================================================\n");

        // TODO: W — Wire the connection to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);


        // TODO: Define the 3 PaymentRequest objects (TXN-A, TXN-B, TXN-C)
        PaymentRequest[] transactions = {
                new PaymentRequest("TXN-A", 250.00, "USD", "US", "US",
                        "Routine supplier payment", "ACC-001", "ACC-002"),
                new PaymentRequest("TXN-B", 12000.00, "USD", "US", "UK",
                        "International consulting fee", "ACC-003", "ACC-004"),
                new PaymentRequest("TXN-C", 75000.00, "USD", "US", "North Korea",
                        "Business consulting services", "ACC-005", "ACC-006"),
        };

        // TODO: For each transaction — I, R, E
        //   I: Build WorkflowOptions with a business ID
        //   R: Create a typed stub
        //   E: Call processPayment() and print the result
        List<String> workflowIds = new ArrayList<>();

        for (PaymentRequest txn : transactions) {
            // I — ID + queue: build WorkflowOptions with a business ID
            String workflowId = "payment-" + txn.getTransactionId();
            workflowIds.add(workflowId);

            // R — Resolve: typed workflow stub
            PaymentProcessingWorkflow workflow = client.newWorkflowStub(
                    PaymentProcessingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(Shared.TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .build());

            // E — Execute: blocks until workflow completes
            System.out.println("  Starting: " + workflowId);
            PaymentResult result = workflow.processPayment(txn);
        }
    }
}
