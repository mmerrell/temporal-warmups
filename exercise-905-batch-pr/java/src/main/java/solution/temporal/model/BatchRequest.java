package solution.temporal.model;

import exercise.model.ReviewRequest;

import java.util.List;

/**
 * Input to the batch workflow - the stack of PRs to review.
 *
 * Q: Why is this a separate class instead of just List<ReviewRequest>?
 * A: We might want to add metadata (like batchId) without changing the workflow signature.
 */
public class BatchRequest {
    public List<ReviewRequest> pullRequests;
    public String batchId;  // Optional: for tracking

    public BatchRequest() {}

    public BatchRequest(List<ReviewRequest> pullRequests, String batchId) {
        this.pullRequests = pullRequests;
        this.batchId = batchId;
    }

    public int getTotalCount() {
        return pullRequests != null ? pullRequests.size() : 0;
    }
}
