import domain.RegistrationResult;
import domain.User;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface VerificationEmailActivity {
    @ActivityMethod
    String sendVerificationEmail(User user, RegistrationResult registrationResult);
}
