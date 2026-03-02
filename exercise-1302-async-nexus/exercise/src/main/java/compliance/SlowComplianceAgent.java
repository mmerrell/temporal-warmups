package compliance;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;

/**
 * [GIVEN] A mock compliance agent used ONLY in Checkpoint 0.
 *
 * This simulates what happened before the async Nexus upgrade:
 * the compliance team's new 3-phase investigation pipeline takes 30+ seconds,
 * but the Payments team's sync Nexus call has a 5-second timeout.
 *
 * Result: every transaction times out. The Payments team pages on-call.
 *
 * This class is NOT used in your actual implementation — it exists to
 * demonstrate the problem that async Nexus solves.
 *
 * Run Checkpoint 0 to see the timeout:
 *   mvn compile exec:java
 */
public class SlowComplianceAgent {

    public InvestigationResult runFullInvestigation(InvestigationRequest request) {
        System.out.println("[SlowComplianceAgent] Starting compliance pipeline for "
                + request.getTransactionId());
        System.out.println("[SlowComplianceAgent] Phase 1/3: OFAC screening (this takes a while...)");

        try {
            Thread.sleep(10000); // Phase 1: 10 seconds
            System.out.println("[SlowComplianceAgent] Phase 2/3: Pattern analysis...");
            Thread.sleep(10000); // Phase 2: 10 seconds
            System.out.println("[SlowComplianceAgent] Phase 3/3: Final review...");
            Thread.sleep(10000); // Phase 3: 10 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // We never get here in practice — the caller times out first
        return new InvestigationResult(request.getTransactionId(), false, "LOW",
                "Investigation complete (but caller already gave up)");
    }
}
