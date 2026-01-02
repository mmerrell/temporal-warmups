package solution.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import solution.domain.VerificationResult;

@WorkflowInterface
public interface EmailVerificationWorkflow {
    @WorkflowMethod
    VerificationResult verifyEmail(String email);
}
