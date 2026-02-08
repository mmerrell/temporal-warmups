package solution.temporal.model;

import exercise.model.ReviewResponse;
import java.util.List;

/**
 * Final result of batch processing.
 * Returned by the workflow when all PRs are done.
 */
public class BatchResult {
    public String batchId;
    public int totalCount;
    public int successCount;
    public int failureCount;

    // Q: Why track continuation count?
    // A: Useful for understanding performance and verifying continue-as-new worked!
    public int continuationCount;

    public long totalDurationMs;
    public String startedAt;
    public String completedAt;

    public List<ReviewResponse> results;
    public List<PRFailure> failures;

    public BatchResult() {}

    /**
     * Factory method to build final result from batch state.
     *
     * Q: Why pass finalChunkDurationMs separately?
     * A: The state's accumulatedDurationMs doesn't include the CURRENT chunk yet.
     *    We need to add the final chunk's time to get the true total.
     *
     * TODO: Implement this method
     */
    public static BatchResult from(BatchState state, BatchRequest request,
                                   String completedAt, long finalChunkDurationMs) {
        BatchResult result = new BatchResult();
        // TODO: Copy batchId from request
        result.batchId = request.batchId;
        // TODO: Get totalCount from request
        result.totalCount = request.getTotalCount();
        // TODO: Copy counts from state
        result.failureCount = state.failedCount;
        result.successCount = state.successfulCount;
        // TODO: continuationCount = state.chunkIndex (each chunk after the first is a continuation)
        result.continuationCount = state.chunkIndex;
        // TODO: totalDurationMs = state.accumulatedDurationMs + finalChunkDurationMs
        result.totalDurationMs = state.accumulatedDurationMs + finalChunkDurationMs;
        // TODO: Copy timestamps and lists
        result.startedAt = state.startedAt;
        result.completedAt = completedAt;
        result.results = state.results;
        result.failures = state.failures;
        return result;
    }
}