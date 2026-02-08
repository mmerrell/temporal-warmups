package solution.temporal;

import exercise.SupportTriageService;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import solution.domain.ApprovalRequest;
import solution.domain.TriageResult;

@WorkflowInterface
public interface SupportTriageWorkflow {
    @WorkflowMethod
    public TriageResult triageTicket(String ticketId, String ticketText);

    @SignalMethod
    void approveTicket(ApprovalRequest request);
}
