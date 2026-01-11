package solution.temporal;

import exercise.model.AgentResult;
import exercise.model.PullRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TestQualityActivity {
    @ActivityMethod
    AgentResult analyze(PullRequest pullRequest);
}
