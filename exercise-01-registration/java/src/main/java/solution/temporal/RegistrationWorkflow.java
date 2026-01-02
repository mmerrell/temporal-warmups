package solution.temporal;

import solution.domain.RegistrationResult;
import solution.domain.User;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;

@WorkflowInterface
public interface RegistrationWorkflow {
    @WorkflowMethod
    List<RegistrationResult> registerUser(List<User> user) throws InterruptedException;
}
