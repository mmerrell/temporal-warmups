package compliance.temporal.activity;

import compliance.ComplianceInvestigator;
import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;

/**
 * [SOLUTION] Thin activity wrapper that delegates to ComplianceInvestigator.
 * Same thin-wrapper pattern as PaymentActivityImpl.
 */
public class ComplianceInvestigationActivityImpl implements ComplianceInvestigationActivity {

    private final ComplianceInvestigator investigator;

    public ComplianceInvestigationActivityImpl(ComplianceInvestigator investigator) {
        this.investigator = investigator;
    }

    @Override
    public InvestigationResult investigate(InvestigationRequest request) {
        return investigator.runFullInvestigation(request);
    }
}
