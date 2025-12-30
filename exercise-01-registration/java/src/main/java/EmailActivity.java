import domain.Email;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface EmailActivity {
    @ActivityMethod
    String sendWelcomeEmail(Email email) throws InterruptedException;
}
