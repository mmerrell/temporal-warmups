package exercise;

import compliance.FraudDetectionAgent;
import compliance.TransactionCategorizerAgent;
import compliance.domain.*;
import payments.PaymentGateway;
import payments.domain.PaymentRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * PRE-TEMPORAL BASELINE
 *
 * This is how the Payments and Compliance teams communicate today - via direct
 * method calls (simulating REST). Run this to see the problems.
 *
 * Problems you'll observe:
 * 1. No retry logic - if fraud detection API fails, the whole payment fails
 * 2. No durability - crash mid-process and you lose everything
 * 3. Tight coupling - Payments team directly calls Compliance team's code
 * 4. No visibility - can't see which step failed or why
 * 5. No approval workflow - high-risk transactions auto-processed
 * 6. No audit trail - no record of compliance decisions
 * 7. Cascading failures - Compliance team outage blocks ALL payments
 */
public class PaymentProcessingService {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final String DASHBOARD_URL = "http://localhost:3000/api/processing/update";

    private static void reportStep(String transactionId, int step, String stepName,
                                    String status, String detail) {
        try {
            String json = String.format(
                "{\"transactionId\":\"%s\",\"step\":%d,\"stepName\":\"%s\",\"status\":\"%s\",\"detail\":\"%s\",\"timestamp\":\"%s\"}",
                transactionId, step, stepName, status,
                detail.replace("\"", "\\\""),
                java.time.Instant.now().toString()
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DASHBOARD_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
            // Dashboard not running — that's fine
        }
    }

    private static void reportReset() {
        try {
            String json = "{\"action\":\"reset\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DASHBOARD_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
            // Dashboard not running — that's fine
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  PAYMENT PROCESSING SERVICE (Pre-Temporal Baseline)");
        System.out.println("  Payments Team <--REST--> Compliance Team");
        System.out.println("==========================================================\n");

        reportReset();

        // These represent two DIFFERENT teams' services
        FraudDetectionAgent fraudAgent = new FraudDetectionAgent();
        TransactionCategorizerAgent categorizerAgent = new TransactionCategorizerAgent();
        PaymentGateway gateway = new PaymentGateway();

        // 5 sample transactions with realistic FinServ patterns
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

        int processed = 0;
        int failed = 0;

        for (PaymentRequest txn : transactions) {
            String separator = "----------------------------------------------------------";
            System.out.println(separator);
            System.out.println("Processing: " + txn.getTransactionId()
                    + " | $" + String.format("%.2f", txn.getAmount())
                    + " | " + txn.getSenderCountry() + " -> " + txn.getReceiverCountry());
            System.out.println(separator);

            try {
                // Step 1: Validate payment (Payments team)
                reportStep(txn.getTransactionId(), 1, "Validate Payment", "in_progress", "Validating payment details");
                boolean valid = gateway.validatePayment(txn);
                if (!valid) {
                    reportStep(txn.getTransactionId(), 1, "Validate Payment", "failed", "Validation failed");
                    System.out.println("  REJECTED: Validation failed\n");
                    failed++;
                    continue;
                }
                reportStep(txn.getTransactionId(), 1, "Validate Payment", "completed", "Validation passed");

                // Step 2: Call Compliance team's fraud detection (REST call in production)
                // PROBLEM: If this fails, the whole payment fails. No retries.
                RiskScreeningRequest screenReq = new RiskScreeningRequest(
                        txn.getTransactionId(), txn.getAmount(),
                        txn.getSenderCountry(), txn.getReceiverCountry(),
                        txn.getDescription());

                reportStep(txn.getTransactionId(), 2, "Fraud Screening", "in_progress", "Calling Compliance team API");
                System.out.println("  Calling Compliance team for fraud screening...");
                RiskScreeningResult riskResult = fraudAgent.screenTransaction(screenReq);
                System.out.println("  Risk: " + riskResult.getRiskLevel()
                        + " (score: " + riskResult.getRiskScore() + ")");
                System.out.println("  Reason: " + riskResult.getReasoning());
                reportStep(txn.getTransactionId(), 2, "Fraud Screening", "completed",
                        "Risk: " + riskResult.getRiskLevel() + " (score: " + riskResult.getRiskScore() + ")");

                // Step 3: Call Compliance team's categorization (another REST call)
                // PROBLEM: If step 2 succeeded but this fails, we wasted the fraud check
                CategoryRequest catReq = new CategoryRequest(
                        txn.getTransactionId(), txn.getAmount(),
                        txn.getDescription(), txn.getSenderCountry(),
                        txn.getReceiverCountry());

                reportStep(txn.getTransactionId(), 3, "Categorize Transaction", "in_progress", "Calling Compliance team API");
                System.out.println("  Calling Compliance team for categorization...");
                TransactionCategory category = categorizerAgent.categorize(catReq);
                System.out.println("  Category: " + category.getCategory()
                        + " (" + category.getSubCategory() + ")");
                System.out.println("  Regulatory: " + category.getRegulatoryFlags());
                reportStep(txn.getTransactionId(), 3, "Categorize Transaction", "completed",
                        category.getCategory() + " (" + category.getSubCategory() + ")");

                // Step 4: Check if approval needed
                // PROBLEM: No way to pause and wait for human approval
                if (riskResult.isRequiresApproval()) {
                    reportStep(txn.getTransactionId(), 4, "Approval Wait", "skipped",
                            "No approval mechanism! Auto-processing high-risk transaction");
                    System.out.println("  ** HIGH RISK - Would need human approval but we can't wait! **");
                    System.out.println("  ** Auto-processing anyway (DANGEROUS!) **");
                } else {
                    reportStep(txn.getTransactionId(), 4, "Approval Wait", "completed", "Skipped (low risk)");
                }

                // Step 5: Execute payment
                reportStep(txn.getTransactionId(), 5, "Execute Payment", "in_progress", "Processing payment");
                String confirmation = gateway.executePayment(txn);
                System.out.println("  COMPLETED: " + confirmation);
                reportStep(txn.getTransactionId(), 5, "Execute Payment", "completed", confirmation);
                processed++;

            } catch (Exception e) {
                System.out.println("  FAILED: " + e.getMessage());
                System.out.println("  ** No retry, no recovery. Transaction lost! **");
                reportStep(txn.getTransactionId(), 0, "Processing", "failed",
                        "FAILED: " + e.getMessage() + " — No retry, no recovery!");
                failed++;
            }
            System.out.println();
        }

        System.out.println("==========================================================");
        System.out.println("  RESULTS");
        System.out.println("==========================================================");
        System.out.println("  Processed: " + processed);
        System.out.println("  Failed: " + failed);
        System.out.println("\n  Problems with this approach:");
        System.out.println("  1. Tight coupling between Payments and Compliance teams");
        System.out.println("  2. No retries on Compliance API failures");
        System.out.println("  3. No human-in-the-loop for high-risk transactions");
        System.out.println("  4. No durability - crash = lost transactions");
        System.out.println("  5. No cross-team visibility or audit trail");
        System.out.println("  6. Compliance outage blocks ALL payments");
        System.out.println("\n  Solution: Temporal Nexus for durable cross-team communication!");
        System.out.println("==========================================================");
    }
}
