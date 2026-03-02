package compliance.temporal.activity;

import compliance.ComplianceInvestigator;
import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  TODO FILE 2 of 7: ComplianceInvestigationActivityImpl — Confidence Builder (10 min)
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: Apply the same thin-wrapper pattern to a new domain.
 * After this file, you'll have confirmed the activity pattern works
 * before tackling the more complex Nexus files.
 *
 * WHAT TO IMPLEMENT:
 *
 *   1. Add a private field: ComplianceInvestigator investigator
 *
 *   2. Add a constructor that accepts a ComplianceInvestigator and assigns it.
 *
 *   3. Implement investigate():
 *      → return investigator.runFullInvestigation(request)
 *
 * CHECKPOINT 1 — After this file:
 *   Run: mvn compile exec:java@activity-test
 *
 *   Expected output:
 *     [INV-ALPHA] Phase 1/3: OFAC screening — checking sanctioned entities...
 *     [INV-ALPHA] Phase 2/3: Pattern analysis — detecting structuring and layering...
 *     [INV-ALPHA] Phase 3/3: Final review — senior analyst sign-off...
 *     Checkpoint 1 PASSED: ComplianceInvestigationActivityImpl delegates correctly.
 *
 *   If you see "Checkpoint 1 FAILED" — check that you're calling
 *   investigator.runFullInvestigation(request), not returning null.
 */
public class ComplianceInvestigationActivityImpl implements ComplianceInvestigationActivity {

    // TODO 1: Add private ComplianceInvestigator field

    // TODO 2: Add constructor that accepts ComplianceInvestigator
    // (The no-arg constructor below is a temporary placeholder so the project compiles.
    //  Replace it with your parameterized constructor once you add the field.)
    public ComplianceInvestigationActivityImpl() {
        // Placeholder — replace with: public ComplianceInvestigationActivityImpl(ComplianceInvestigator investigator)
    }

    @Override
    public InvestigationResult investigate(InvestigationRequest request) {
        return null; // TODO 3: delegate to investigator.runFullInvestigation(request)
    }
}
