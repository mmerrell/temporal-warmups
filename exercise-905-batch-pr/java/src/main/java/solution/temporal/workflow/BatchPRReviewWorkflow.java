package solution.temporal.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import solution.temporal.model.*;

@WorkflowInterface
public interface BatchPRReviewWorkflow {

    /**
     * Process a batch of PRs.
     *
     * Q: Why TWO parameters instead of just BatchRequest?
     *
     * Think about what happens on continue-as-new:
     *   - First call:        processBatch(request, null)
     *   - After continue:    processBatch(request, stateFromPreviousRun)
     *
     * The STATE parameter is how we pass the "baton"!
     * On the first run, the caller passes null.
     * On continuations, Temporal passes the state from the previous run.
     */
    @WorkflowMethod
    BatchResult processBatch(BatchRequest request, BatchState state);

    /**
     * Query method - check progress without affecting the workflow.
     *
     * Q: How is a query different from a signal?
     * A: Queries are READ-ONLY. They cannot modify workflow state.
     *    They're like asking "what's your status?" without changing anything.
     *
     * Test with CLI:
     *   temporal workflow query --workflow-id=YOUR_ID --type=getProgress
     */
    @QueryMethod
    BatchProgress getProgress();
}