package solution.temporal.model;

import exercise.model.ReviewRequest;

import java.util.List;

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
