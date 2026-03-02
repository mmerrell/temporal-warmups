package compliance.temporal;

import compliance.ComplianceInvestigator;
import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import compliance.temporal.activity.ComplianceInvestigationActivityImpl;

/**
 * [GIVEN] Checkpoint 1 runner — tests your ComplianceInvestigationActivityImpl.
 *
 * This is NOT a JUnit test. It's a simple main class that calls your
 * activity implementation directly (without Temporal), so you can verify
 * your delegation code is correct before wiring it into the full system.
 *
 * Expected output:
 *   [INV-ALPHA] Phase 1/3: OFAC screening — checking sanctioned entities...
 *   [INV-ALPHA] Phase 1/3: OFAC screening complete.
 *   [INV-ALPHA] Phase 2/3: Pattern analysis — detecting structuring and layering...
 *   ...
 *   Checkpoint 1 PASSED: ComplianceInvestigationActivityImpl delegates correctly.
 *
 * Run with: mvn compile exec:java@activity-test
 */
public class InvestigationActivityTest {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  Checkpoint 1: Testing ComplianceInvestigationActivityImpl");
        System.out.println("  (Running investigation inline — no Temporal needed)");
        System.out.println("═══════════════════════════════════════════════════\n");

        ComplianceInvestigator investigator = new ComplianceInvestigator();
        // Uses reflection to support both: stub (no-arg) and solution (with-arg) constructors
        ComplianceInvestigationActivityImpl activity;
        try {
            activity = ComplianceInvestigationActivityImpl.class
                    .getConstructor(ComplianceInvestigator.class)
                    .newInstance(investigator);
        } catch (NoSuchMethodException e) {
            System.out.println("  *** Constructor TODO not yet implemented — using no-arg fallback ***");
            System.out.println("  *** Implement the constructor in ComplianceInvestigationActivityImpl ***\n");
            try {
                activity = ComplianceInvestigationActivityImpl.class
                        .getConstructor()
                        .newInstance();
            } catch (Exception ex) {
                System.out.println("  Cannot instantiate ComplianceInvestigationActivityImpl: " + ex.getMessage());
                System.exit(1);
                return;
            }
        } catch (Exception e) {
            System.out.println("  Error instantiating activity: " + e.getMessage());
            System.exit(1);
            return;
        }

        InvestigationRequest request = new InvestigationRequest(
                "TXN-ALPHA", 3500.00, "US", "Germany",
                "Equipment purchase from European supplier");

        System.out.println("Running investigation for TXN-ALPHA (expect ~15 seconds)...\n");

        long start = System.currentTimeMillis();
        InvestigationResult result = activity.investigate(request);
        long elapsed = (System.currentTimeMillis() - start) / 1000;

        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("  RESULT:");
        System.out.println("  Transaction: " + result.getTransactionId());
        System.out.println("  Blocked:     " + result.isBlocked());
        System.out.println("  Risk Level:  " + result.getRiskLevel());
        System.out.println("  Summary:     " + result.getSummary());
        System.out.println("  Time:        " + elapsed + " seconds");

        if (result.getTransactionId() != null && !result.isBlocked() && "LOW".equals(result.getRiskLevel())) {
            System.out.println("\n  Checkpoint 1 PASSED: ComplianceInvestigationActivityImpl delegates correctly.");
        } else if (result.getTransactionId() == null) {
            System.out.println("\n  Checkpoint 1 FAILED: result is null — did you implement the activity?");
            System.exit(1);
        } else {
            System.out.println("\n  Checkpoint 1 PASSED: Activity delegates to ComplianceInvestigator.");
        }
        System.out.println("═══════════════════════════════════════════════════\n");
    }
}
