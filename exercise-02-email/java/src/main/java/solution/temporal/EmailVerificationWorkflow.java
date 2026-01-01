package solution.temporal;

import io.temporal.workflow.WorkflowInterface;
import solution.domain.VerificationResult;

@WorkflowInterface
public interface EmailVerificationWorkflow {
    VerificationResult verifyEmail(String email);
}
