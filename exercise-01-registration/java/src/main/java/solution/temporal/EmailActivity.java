package solution.temporal;

import solution.domain.Email;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface EmailActivity {
    @ActivityMethod
    void sendWelcomeEmail(Email email) throws InterruptedException;
}
