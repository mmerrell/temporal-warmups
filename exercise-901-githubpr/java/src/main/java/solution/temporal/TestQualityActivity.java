package solution.temporal;

import exercise.model.AgentResult;
import exercise.model.ReviewRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TestQualityActivity {
    @ActivityMethod
    AgentResult analyze(ReviewRequest pullRequest);
}
