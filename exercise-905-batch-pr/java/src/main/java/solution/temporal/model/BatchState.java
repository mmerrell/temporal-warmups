package solution.temporal.model;

import exercise.model.ReviewResponse;

import java.util.ArrayList;
import java.util.List;

public class BatchState {
    public int processedCount;
    public int successfulCount;
    public int failedCount;
    public int chunkIndex;
    public long accumulatedDurationMs;
    public String startedAt;
    public List<ReviewResponse> result;
    public List<PRFailure> failures;

    public BatchState(){
        result = new ArrayList<>();
        failures = new ArrayList<>();
    }

    /**
     * Factory method - creates initial state for a brand new batch.
     * Called only on the FIRST run, not on continuations.
     */
    public static BatchState initial(String startedAt){
        BatchState state = new BatchState();
        state.startedAt = startedAt;
        return state;
    }
    /**
     * Call this after successfully processing a PR.
     */
    public void recordSuccess(ReviewResponse response) {
        result.add(response);
        processedCount++;
        successfulCount++;
    }

    /**
     * Call this after a PR fails.
     */
    public void recordFailure(PRFailure failure) {
        failures.add(failure);
        processedCount++;
        failedCount++;
    }
}
