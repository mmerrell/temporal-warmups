package compliance.temporal.activity;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * [GIVEN] Activity interface for the 3-phase compliance investigation.
 * Implemented by you in ComplianceInvestigationActivityImpl.java.
 *
 * This activity runs on the Compliance team's worker. It delegates
 * to ComplianceInvestigator which performs the actual 3-phase logic.
 */
@ActivityInterface
public interface ComplianceInvestigationActivity {
    @ActivityMethod
    InvestigationResult investigate(InvestigationRequest request);
}
