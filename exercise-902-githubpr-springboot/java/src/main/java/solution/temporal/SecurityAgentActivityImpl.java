package solution.temporal;

import exercise.agents.SecurityAgent;
import exercise.model.AgentResult;
import exercise.model.ReviewRequest;

public class SecurityAgentActivityImpl implements SecurityQualityActivity {
    private final SecurityAgent securityAgent;

    public SecurityAgentActivityImpl(SecurityAgent securityAgent) {
        this.securityAgent = securityAgent;
    }

    @Override
    public AgentResult analyze(ReviewRequest pullRequest) {
        return securityAgent.analyze(pullRequest.prTitle,
                pullRequest.prDescription, pullRequest.diff);
    }
}
