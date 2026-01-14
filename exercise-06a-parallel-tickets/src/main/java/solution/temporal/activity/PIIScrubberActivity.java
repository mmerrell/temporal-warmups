package solution.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PIIScrubberActivity {
    @ActivityMethod
    String scrubPII(String ticketText);
}
