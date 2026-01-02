package solution.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface EmailSendingActivity {
    @ActivityMethod
    String sendVerificationEmail(String email, String token);
}
