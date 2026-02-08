package solution.temporal.activity;

import exercise.model.AgentResult;
import exercise.model.ReviewRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SecurityQualityActivity {
    @ActivityMethod(name = "AnalyzeSecurityQuality")
    AgentResult analyze(ReviewRequest pullRequest);
}
