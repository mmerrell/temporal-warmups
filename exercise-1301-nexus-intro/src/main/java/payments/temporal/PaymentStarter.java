package payments.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import payments.domain.PaymentRequest;
import payments.domain.PaymentResult;

/**
 * YOUR TURN: Start 3 payment workflows sequentially.
 *
 * START pattern — Starters START workflows:
 *   S — Service:  Connect to Temporal
 *   T — Target:   Build WorkflowOptions (task queue + workflow ID)
 *   A — Acquire:  Create a typed workflow stub
 *   R — Run:      Call the workflow method — blocks until the workflow completes
 *   T — Track:    Print the result
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
 *   1. Connect to Temporal (S)
 *   2. Define the 3 PaymentRequest objects above
 *   3. For each transaction (T, A, R, T):
 *      - Build WorkflowOptions with task queue "payments-processing"
 *        and a business ID workflow ID
 *      - Create a typed workflow stub from the client
 *      - Call processPayment() on the stub — this blocks until done
 *      - Print the result: status, riskLevel, explanation, confirmationNumber
 */
public class PaymentStarter {

    private static final String TASK_QUEUE = "payments-processing";

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  PAYMENT STARTER — Exercise 1301: Nexus Intro");
        System.out.println("  Running 3 transactions through Temporal + Nexus");
        System.out.println("==========================================================\n");

        // TODO: S — Connect to Temporal

        // TODO: Define the 3 PaymentRequest objects (TXN-A, TXN-B, TXN-C)

        // TODO: For each transaction — T, A, R, T
        //   Build WorkflowOptions with a business ID
        //   Create a typed stub
        //   Call processPayment() and print the result

        throw new UnsupportedOperationException("TODO: implement PaymentStarter");
    }
}
