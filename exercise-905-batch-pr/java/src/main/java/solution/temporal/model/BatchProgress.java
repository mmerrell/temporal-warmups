package solution.temporal.model;

import java.util.Objects;

/**
 * Current progress snapshot - returned by @QueryMethod.
 *
 * Q: Why a separate class from BatchState?
 * A: BatchState is internal (the baton). BatchProgress is external (what users see).
 *    We might not want to expose all internal state to queries.
 */
public class BatchProgress {
    public int totalCount;       // Total PRs in batch
    public int processedCount;   // How many handled so far
    public int successCount;     // How many succeeded
    public int failureCount;     // How many failed
    public int currentChunk;     // Which continuation we're on
    public double percentComplete;
    public String currentPrTitle; // What's being processed RIGHT NOW?
    public String status;         // "PROCESSING", "COMPLETED", etc.

    public BatchProgress() {}

    public static BatchProgress from(BatchState state, int totalCount,
                                     String currentPrTitle, long currentChunkDurationMs) {
        BatchProgress progress = new BatchProgress();
        // TODO: Copy fields from state\
        progress.totalCount = totalCount;
        progress.currentPrTitle = currentPrTitle;
        // TODO: Calculate percentComplete (careful: division by zero!)
        if(progress.totalCount != 0){
            progress.percentComplete = (progress.processedCount * 100.0) / progress.totalCount;
        }
        // TODO: Set status based on whether we're done
        progress.status = Status.IN_PROGRESS;
        if(!Objects.equals(progress.currentPrTitle, "")){
            progress.status = Status.PROCESSING;
        }
        return progress;
    }
}
