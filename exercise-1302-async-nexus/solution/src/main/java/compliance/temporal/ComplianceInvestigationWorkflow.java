package compliance.temporal;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * [GIVEN] Workflow interface for the compliance investigation workflow.
 * Implemented by you in ComplianceInvestigationWorkflowImpl.java.
 *
 * This workflow runs on the Compliance team's worker. It orchestrates
 * the 3-phase investigation via ComplianceInvestigationActivity.
 *
 * IMPORTANT: The method name here must exactly match what you use in
 * ComplianceNexusServiceImpl when building the fromWorkflowMethod() handler.
 *
 *   client.newWorkflowStub(ComplianceInvestigationWorkflow.class, ...)::investigate
 *                                                                        ^^^^^^^^^^^
 *                                                          must match this method name
 */
@WorkflowInterface
public interface ComplianceInvestigationWorkflow {
    @WorkflowMethod
    InvestigationResult investigate(InvestigationRequest request);
}
