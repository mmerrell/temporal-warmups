package solution.temporal;

import solution.domain.User;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface UserRecordCreationActivity {
    @ActivityMethod
    String createUserRecord(User user) throws InterruptedException;
}
