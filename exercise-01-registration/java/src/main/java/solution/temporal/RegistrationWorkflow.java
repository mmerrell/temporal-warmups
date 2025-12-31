package solution.temporal;

import solution.domain.RegistrationResult;
import solution.domain.User;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RegistrationWorkflow {
    @WorkflowMethod
    RegistrationResult registerUser(User user) throws InterruptedException;
}
