import domain.RegistrationResult;
import domain.User;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RegistrationWorkflow {
    @WorkflowMethod
    RegistrationResult registerUser(User user) throws InterruptedException;
}
