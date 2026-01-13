package solution.temporal;

import exercise.model.ReviewRequest;
import exercise.model.ReviewResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PRReviewWorkflow {
    @WorkflowMethod
    ReviewResponse review(ReviewRequest request);
}
