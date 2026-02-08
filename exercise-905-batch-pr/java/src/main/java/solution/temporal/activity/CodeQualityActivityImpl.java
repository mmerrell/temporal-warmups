package solution.temporal.activity;

import exercise.agents.CodeQualityAgent;
import exercise.model.AgentResult;
import exercise.model.ReviewRequest;

public class CodeQualityActivityImpl implements CodeQualityActivity {
    private final CodeQualityAgent codeQualityAgent;

    public CodeQualityActivityImpl(CodeQualityAgent codeQualityAgent) {
        this.codeQualityAgent = codeQualityAgent;
    }

    @Override
    public AgentResult analyze(ReviewRequest pullRequest) {
        return codeQualityAgent.analyze(pullRequest.prTitle,
                pullRequest.prDescription, pullRequest.diff);
    }
}
