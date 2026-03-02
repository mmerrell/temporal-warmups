package exercise;

import compliance.ComplianceAgent;
import compliance.domain.ComplianceRequest;
import compliance.domain.ComplianceResult;
import payments.PaymentGateway;
import payments.domain.PaymentRequest;

/**
 * PRE-TEMPORAL BASELINE
 *
 * This shows how Payments and Compliance teams communicate today —
 * via a direct method call (simulating a REST API call).
 *
 * Run this first to see the problems, then implement the Temporal solution.
 *
 * ── Problems with this approach ──────────────────────────────────
 *
 *   1. TIGHT COUPLING: Payments team directly calls Compliance team's code.
 *      In real life this is an HTTP call. If Compliance is down, all payments fail.
 *
 *   2. NO RETRIES: If checkCompliance() throws, the payment is lost.
 *      You'd need to write retry loops manually.
 *
 *   3. NO DURABILITY: If the process crashes between steps 1 and 3,
 *      you don't know which step succeeded. No recovery path.
 *
 *   4. NO VISIBILITY: You can't see which step failed or how long each took.
 *
 *   5. NO AUDIT TRAIL: Who made the compliance decision? When? Why?
 *      All lost when the process ends.
 *
 * ── How Temporal + Nexus fixes these ─────────────────────────────
 *
 *   After implementing the exercise:
 *   - Each step is a durable activity (auto-retried on failure)
 *   - The compliance call is a Nexus operation (durable cross-team RPC)
 *   - Every step is visible in the Temporal UI with timing and inputs/outputs
 *   - The workflow event history IS the audit trail
 *   - Crash and restart? The workflow replays from the last checkpoint
 */
public class PaymentProcessingService {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  PAYMENT PROCESSING (Pre-Temporal Baseline)");
        System.out.println("  Payments Team calls Compliance Team directly");
        System.out.println("==========================================================\n");

        ComplianceAgent complianceAgent = new ComplianceAgent();
        PaymentGateway gateway = new PaymentGateway();

        PaymentRequest[] transactions = {
            new PaymentRequest("TXN-A", 250.00, "USD", "US", "US",
                    "Routine supplier payment", "ACC-001", "ACC-002"),
            new PaymentRequest("TXN-B", 12000.00, "USD", "US", "UK",
                    "International consulting fee", "ACC-003", "ACC-004"),
            new PaymentRequest("TXN-C", 75000.00, "USD", "US", "North Korea",
                    "Business consulting services", "ACC-005", "ACC-006"),
        };

        for (PaymentRequest txn : transactions) {
            System.out.println("----------------------------------------------------------");
            System.out.println("Processing: " + txn.getTransactionId()
                    + " | $" + String.format("%.2f", txn.getAmount())
                    + " | " + txn.getSenderCountry() + " -> " + txn.getReceiverCountry());

            try {
                // Step 1: Validate payment (Payments team)
                boolean valid = gateway.validatePayment(txn);
                if (!valid) {
                    System.out.println("  REJECTED: Validation failed — payment lost, no retry\n");
                    continue;
                }
                System.out.println("  Step 1 passed: validation OK");

                // Step 2: Call Compliance team for risk check
                // PROBLEM: Direct call — no retries, no durability, tight coupling
                ComplianceRequest compReq = new ComplianceRequest(
                        txn.getTransactionId(), txn.getAmount(),
                        txn.getSenderCountry(), txn.getReceiverCountry(),
                        txn.getDescription());

                System.out.println("  Step 2: calling Compliance team...");
                ComplianceResult compliance = complianceAgent.checkCompliance(compReq);
                System.out.println("  Compliance result: " + compliance.getRiskLevel()
                        + " | approved=" + compliance.isApproved());
                System.out.println("  Reason: " + compliance.getExplanation());

                if (!compliance.isApproved()) {
                    System.out.println("  DECLINED: Compliance blocked this transaction");
                    System.out.println("  ** No audit trail — why was it declined? Who decided? **\n");
                    continue;
                }

                // Step 3: Execute payment
                // PROBLEM: If step 2 succeeded but this crashes, we don't know
                System.out.println("  Step 3: executing payment...");
                String confirmation = gateway.executePayment(txn);
                System.out.println("  COMPLETED: " + confirmation);

            } catch (Exception e) {
                System.out.println("  FAILED: " + e.getMessage());
                System.out.println("  ** No retry logic — transaction lost! **");
            }
            System.out.println();
        }

        System.out.println("==========================================================");
        System.out.println("  Problems observed:");
        System.out.println("  1. Direct coupling between Payments and Compliance");
        System.out.println("  2. No retries on Compliance API failure");
        System.out.println("  3. No durability — crash = lost transaction");
        System.out.println("  4. No visibility into which step failed");
        System.out.println("  5. No audit trail for compliance decisions");
        System.out.println();
        System.out.println("  Solution: Temporal Nexus for durable cross-team calls!");
        System.out.println("==========================================================");
    }
}
