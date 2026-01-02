package solution.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import solution.domain.*;

@ActivityInterface
public interface UserValidatorActivity {
    @ActivityMethod
    boolean validateUserData(User user) throws InterruptedException;
}