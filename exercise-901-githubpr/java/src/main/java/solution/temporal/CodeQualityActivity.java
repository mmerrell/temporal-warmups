package solution.temporal;

import exercise.model.AgentResult;
import exercise.model.ReviewRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CodeQualityActivity {
    // NOTE - we pulled out the parameters into an object
    @ActivityMethod
    AgentResult analyze(ReviewRequest pullRequest);
}
