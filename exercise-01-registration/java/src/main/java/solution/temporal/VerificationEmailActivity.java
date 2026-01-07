package solution.temporal;

import solution.domain.RegistrationResult;
import solution.domain.User;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface VerificationEmailActivity {
    @ActivityMethod
    String sendVerificationEmail(User user, RegistrationResult registrationResult) throws InterruptedException;
}
