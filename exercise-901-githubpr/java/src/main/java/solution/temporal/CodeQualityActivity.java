package solution.temporal;

import exercise.model.AgentResult;
import exercise.model.PullRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CodeQualityActivity {
    // NOTE - we pulled out the parameters into an object
    @ActivityMethod
    AgentResult analyze(PullRequest pullRequest);
}
