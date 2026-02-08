package solution.temporal.activity;

import exercise.agents.SecurityAgent;
import exercise.model.AgentResult;
import exercise.model.ReviewRequest;

public class SecurityQualityActivityImpl implements SecurityQualityActivity {
    private final SecurityAgent securityAgent;

    public SecurityQualityActivityImpl(SecurityAgent securityAgent) {
        this.securityAgent = securityAgent;
    }

    @Override
    public AgentResult analyze(ReviewRequest pullRequest) {
        return securityAgent.analyze(pullRequest.prTitle,
                pullRequest.prDescription, pullRequest.diff);
    }
}
