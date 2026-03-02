package exercise;

import compliance.SlowComplianceAgent;
import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;

import java.util.concurrent.*;

/**
 * [GIVEN] Checkpoint 0: The Problem — sync call to a slow compliance pipeline.
 *
 * The Compliance team upgraded their AI pipeline from a 2-second check
 * to a 3-phase forensic investigation (OFAC → pattern analysis → final review).
 * Total time: 30 seconds. But the Payments team's SLA is 5 seconds.
 *
 * This class demonstrates what happens without async Nexus:
 *   - Sync call with a 5-second timeout
 *   - SlowComplianceAgent takes 30 seconds
 *   - Every transaction times out
 *   - No retry. No audit trail. Transaction is lost.
 *
 * Run this first, BEFORE implementing anything:
 *   mvn compile exec:java
 *
 * You should see a timeout exception. That's the problem you're solving.
 */
public class PaymentProcessingService {

    private static final int COMPLIANCE_TIMEOUT_SECONDS = 5;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   CHECKPOINT 0: The Sync Timeout Problem                ║");
        System.out.println("║   Payments team calling slow Compliance pipeline        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        SlowComplianceAgent agent = new SlowComplianceAgent();
        InvestigationRequest request = new InvestigationRequest(
                "TXN-ALPHA", 3500.00, "US", "Germany",
                "Equipment purchase from European supplier");

        System.out.println("  Calling compliance team for TXN-ALPHA...");
        System.out.println("  Timeout: " + COMPLIANCE_TIMEOUT_SECONDS + " seconds");
        System.out.println("  Compliance pipeline takes: ~30 seconds");
        System.out.println();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<InvestigationResult> future = executor.submit(
                () -> agent.runFullInvestigation(request));

        try {
            InvestigationResult result = future.get(COMPLIANCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            // We never reach here
            System.out.println("  Result: " + result);

        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  ERROR: java.util.concurrent.TimeoutException           ║");
            System.out.println("║  Compliance check exceeded " + COMPLIANCE_TIMEOUT_SECONDS + "-second SLA              ║");
            System.out.println("║                                                          ║");
            System.out.println("║  Transaction TXN-ALPHA: LOST                            ║");
            System.out.println("║  Retries: 0                                             ║");
            System.out.println("║  Audit trail: none                                      ║");
            System.out.println("║  On-call engineer: paged                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");
            System.out.println("  This is what async Nexus solves.");
            System.out.println("  Instead of blocking for 30 seconds, Temporal holds the handle");
            System.out.println("  and resumes the Payments workflow when the investigation completes.");
            System.out.println("  Read the README to see how.");

        } catch (Exception e) {
            System.out.println("  Unexpected error: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }
}
