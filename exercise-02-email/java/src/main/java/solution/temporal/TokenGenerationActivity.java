package solution.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TokenGenerationActivity {
    @ActivityMethod
    String generateToken(String email);
}
