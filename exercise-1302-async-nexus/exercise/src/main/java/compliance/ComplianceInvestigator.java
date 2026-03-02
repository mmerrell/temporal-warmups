package compliance;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import io.temporal.activity.Activity;

/**
 * [GIVEN] The 3-phase forensic investigation logic.
 *
 * Phase 1: OFAC Screening    — checks sanctioned entities and countries
 * Phase 2: Pattern Analysis  — detects structuring, layering, unusual routing
 * Phase 3: Final Review      — senior analyst sign-off with risk scoring
 *
 * Investigation durations per transaction:
 *   TXN-ALPHA ($3,500  US→Germany):    ~15 seconds total
 *   TXN-BETA  ($47,500 US→Caymans):    ~35 seconds total
 *   TXN-GAMMA ($180,000 US→Iran):      ~60 seconds total
 *
 * Students use this class as-is — focus is on wiring it into Temporal activities,
 * not the investigation logic itself.
 */
public class ComplianceInvestigator {

    public InvestigationResult runFullInvestigation(InvestigationRequest request) {
        String txn = request.getTransactionId();
        String prefix = "[INV-" + txn.replace("TXN-", "") + "]";

        // Determine investigation profile based on transaction
        int[] phaseDurations = getInvestigationProfile(request);

        try {
            // Phase 1: OFAC Screening
            System.out.println(prefix + " Phase 1/3: OFAC screening — checking sanctioned entities...");
            heartbeat("Phase 1/3: OFAC screening");
            sleep(phaseDurations[0]);
            System.out.println(prefix + " Phase 1/3: OFAC screening complete.");

            // Phase 2: Pattern Analysis
            System.out.println(prefix + " Phase 2/3: Pattern analysis — detecting structuring and layering...");
            heartbeat("Phase 2/3: Pattern analysis");
            sleep(phaseDurations[1]);
            System.out.println(prefix + " Phase 2/3: Pattern analysis complete.");

            // Phase 3: Final Review
            System.out.println(prefix + " Phase 3/3: Final review — senior analyst sign-off...");
            heartbeat("Phase 3/3: Final review");
            sleep(phaseDurations[2]);
            System.out.println(prefix + " Phase 3/3: Final review complete.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Investigation interrupted for " + txn, e);
        }

        return buildResult(request);
    }

    private int[] getInvestigationProfile(InvestigationRequest request) {
        double amount = request.getAmount();
        String dest = request.getReceiverCountry();

        // TXN-GAMMA: sanctioned country — full 60-second deep investigation
        if (dest.equalsIgnoreCase("Iran") || dest.equalsIgnoreCase("North Korea")
                || dest.equalsIgnoreCase("Cuba") || dest.equalsIgnoreCase("Syria")) {
            return new int[]{15000, 20000, 25000}; // 60s total
        }
        // TXN-BETA: large international — 35-second investigation
        if (amount >= 10000 && !request.getSenderCountry().equalsIgnoreCase(dest)) {
            return new int[]{10000, 15000, 10000}; // 35s total
        }
        // TXN-ALPHA: routine — 15-second investigation
        return new int[]{5000, 5000, 5000}; // 15s total
    }

    private InvestigationResult buildResult(InvestigationRequest request) {
        String dest = request.getReceiverCountry();
        double amount = request.getAmount();

        // Sanctioned country — always blocked
        if (dest.equalsIgnoreCase("Iran") || dest.equalsIgnoreCase("North Korea")
                || dest.equalsIgnoreCase("Cuba") || dest.equalsIgnoreCase("Syria")) {
            return new InvestigationResult(request.getTransactionId(), true, "CRITICAL",
                    "Transaction blocked: destination country is OFAC-sanctioned. "
                    + "All 3 investigation phases confirm: payment to " + dest + " violates regulatory requirements.");
        }

        // Large international — medium risk, approved
        if (amount >= 10000) {
            return new InvestigationResult(request.getTransactionId(), false, "MEDIUM",
                    "Transaction approved with elevated monitoring. Amount $"
                    + String.format("%.0f", amount) + " to " + dest
                    + " triggers enhanced due diligence. No structuring patterns detected.");
        }

        // Routine — low risk, approved
        return new InvestigationResult(request.getTransactionId(), false, "LOW",
                "Transaction approved. Routine transfer to " + dest
                + ". All 3 investigation phases passed. No flags raised.");
    }

    private void heartbeat(String phase) {
        try {
            // Heartbeat so Temporal knows the activity is still alive during long phases
            Activity.getExecutionContext().heartbeat(phase);
        } catch (Exception e) {
            // Not running inside a Temporal activity (e.g., test runner) — ignore
        }
    }

    private void sleep(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
