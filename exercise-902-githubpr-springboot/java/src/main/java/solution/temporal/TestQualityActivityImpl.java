package solution.temporal;

import exercise.agents.TestQualityAgent;
import exercise.model.AgentResult;
import exercise.model.ReviewRequest;

public class TestQualityActivityImpl implements TestQualityActivity {
    private final TestQualityAgent testQualityAgent;

    public TestQualityActivityImpl(TestQualityAgent testQualityAgent) {
        this.testQualityAgent = testQualityAgent;
    }

    @Override
    public AgentResult analyze(ReviewRequest pullRequest) {
        return testQualityAgent.analyze(pullRequest.prTitle,
                pullRequest.prDescription, pullRequest.diff,
                pullRequest.testSummary);
    };
}
