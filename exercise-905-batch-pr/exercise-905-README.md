# Exercise 905: Batch Processing + Continue-as-New

## Learning Objectives

By the end of this exercise, you will understand:
- Why Temporal workflows have event history limits
- What Continue-as-New is and when to use it
- How to design state that survives workflow restarts
- How to implement the "relay race" pattern for large batch processing

---

## Part 1: Understanding the Problem

### Why Does Temporal Have a History Limit?

Every action in a Temporal workflow creates an **event** in the history:
- Workflow started → 1 event
- Activity scheduled → 1 event
- Activity completed → 1 event
- Timer started → 1 event
- etc.

Temporal stores this history to enable **replay** - if a worker crashes, Temporal replays the history to restore the workflow's exact state. This is the magic that makes workflows durable.

**But there's a cost:** The entire history must fit in memory during replay.

```
┌─────────────────────────────────────────────────────────────┐
│                    TEMPORAL'S LIMIT                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Soft limit: ~10,000 events  (Temporal starts warning)     │
│   Hard limit: ~50,000 events  (Workflow terminates!)        │
│                                                              │
│   Think of it like RAM - fast but finite                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### The Math Problem

In Exercise 901, each PR review creates approximately **11 events**:
- 3 activities × (schedule + start + complete) = 9 events
- Plus workflow overhead = ~11 events total

| Batch Size | Events | Problem |
|------------|--------|---------|
| 10 PRs | ~110 events | Fine |
| 100 PRs | ~1,100 events | Getting heavy |
| 1,000 PRs | ~11,000 events | Exceeds soft limit |
| 5,000 PRs | ~55,000 events | Workflow terminated! |

**Question:** How do we process 10,000 PRs if we can only fit ~4,500 in one workflow run?

---

## Part 2: The Solution - Continue-as-New

### The Relay Race Metaphor

Imagine a relay race where:
- Each runner (workflow run) can only run 1/4 of the track
- At the handoff point, the runner passes a **baton** to the next runner
- The baton contains: current position, lap count, split times
- The race continues seamlessly - same race, different runners

```
┌─────────────────────────────────────────────────────────────────┐
│                    THE RELAY RACE PATTERN                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Run 1                    Run 2                    Run 3         │
│  ┌──────────┐            ┌──────────┐            ┌──────────┐   │
│  │ PRs 1-25 │  ──baton── │PRs 26-50 │  ──baton── │PRs 51-75 │   │
│  │ 275 evts │  ────────► │ 275 evts │  ────────► │ 275 evts │   │
│  └──────────┘            └──────────┘            └──────────┘   │
│       │                       │                       │          │
│       ▼                       ▼                       ▼          │
│  Fresh history           Fresh history           Fresh history   │
│  Same workflow ID        Same workflow ID        Same workflow ID│
│                                                                  │
│  The "baton" (BatchState) carries:                              │
│  • How many PRs done (processedCount)                           │
│  • Results collected so far                                      │
│  • Total elapsed time                                           │
│  • Which continuation we're on                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### What Happens When You Call Continue-as-New?

```java
Workflow.continueAsNew(request, state);  // THE MAGIC LINE
```

When this line executes:

1. **Current workflow run ends** immediately (code after this line NEVER runs)
2. **Temporal creates a new workflow run** with:
   - Same workflow ID
   - Fresh, empty event history
   - The arguments you passed (`request`, `state`)
3. **Your workflow method starts over** from the beginning
4. **Temporal UI links the runs** in a "Continue-As-New Chain"

**Critical insight:** It's like calling `main()` again, but you get to pass data forward.

---

## Part 3: Building the Model Classes

### Step 1: PRFailure - Tracking Problems

**Purpose:** When a PR review fails, we want to know which one and why.

**Create the file:** `solution/temporal/model/PRFailure.java`

**Guiding questions:**
```java
package solution.temporal.model;

/**
 * Information about a PR that failed to review.
 *
 * Q: What information would help you debug a failed PR later?
 * Think about:
 *   - Which PR failed? (by index? by title?)
 *   - What went wrong? (error message)
 *   - When did it fail? (timestamp)
 */
public class PRFailure {
    // Q: If you have 100 PRs and #47 fails, how do you identify it?
    // Hint: Store the index (0-based position in the batch)

    // Q: The index is good for code, but what about humans reading logs?
    // Hint: Store the PR title too

    // Q: What went wrong?
    // Hint: Store the exception message

    // Q: When did it happen?
    // Hint: Store a timestamp (as ISO-8601 String for serialization)

    // Don't forget: No-arg constructor required for Temporal serialization!
    public PRFailure() {}

    // Convenience constructor with all fields
    public PRFailure(int prIndex, String prTitle, String errorMessage, String failedAt) {
        // TODO: assign all fields
    }
}
```

---

### Step 2: BatchState - The Baton (Most Important!)

**Purpose:** This is the heart of Continue-as-New. It carries everything needed to resume after a restart.

**Create the file:** `solution/temporal/model/BatchState.java`

**Key question to ask yourself:** "If my workflow restarted RIGHT NOW, what would I need to continue?"

**Guiding questions for fields:**
```java
package solution.temporal.model;

import exercise.model.ReviewResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * The "baton" passed between continue-as-new runs.
 *
 * CRITICAL: Everything you need to resume MUST be in this class!
 * Local variables in your workflow are LOST on continue-as-new.
 */
public class BatchState {

    // ─────────────────────────────────────────────────────────
    // PROGRESS TRACKING
    // ─────────────────────────────────────────────────────────

    // Q: How do you know which PR to process next?
    // Think about the loop: for (int i = ???; i < allPRs.size(); i++)
    // Hint: You need to track how many PRs you've already handled
    public int processedCount;

    // Q: How many reviews succeeded vs failed?
    // Hint: You'll want separate counts for success and failure
    public int successCount;
    public int failureCount;

    // Q: How do you know this is continuation #3 vs #1?
    // Hint: Increment this each time you call continueAsNew
    public int chunkIndex;

    // ─────────────────────────────────────────────────────────
    // TIMING
    // ─────────────────────────────────────────────────────────

    // Q: Why can't we just use (endTime - startTime) at the end?
    // Think: Each continuation starts fresh. Run 1 takes 30s, Run 2 takes 30s...
    // If you only time Run 2, you miss Run 1's time!
    // Hint: Accumulate duration across all runs
    public long accumulatedDurationMs;

    // Q: When did the ENTIRE batch start (not just this continuation)?
    // Hint: Set this only on the first run, then carry it forward
    public String startedAt;

    // ─────────────────────────────────────────────────────────
    // RESULTS
    // ─────────────────────────────────────────────────────────

    // Q: Where do successful review results go?
    public List<ReviewResponse> results;

    // Q: Where do we track which PRs failed and why?
    public List<PRFailure> failures;

    // ─────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ─────────────────────────────────────────────────────────

    /**
     * No-arg constructor required for Temporal serialization.
     *
     * Q: What happens if you call results.add(...) when results is null?
     * A: NullPointerException!
     *
     * IMPORTANT: Initialize your lists here!
     */
    public BatchState() {
        // TODO: Initialize the lists!
        // this.results = new ArrayList<>();
        // this.failures = ???
    }

    /**
     * Factory method - creates initial state for a brand new batch.
     * Called only on the FIRST run, not on continuations.
     */
    public static BatchState initial(String startedAt) {
        BatchState state = new BatchState();
        state.startedAt = startedAt;
        // Counts start at 0, lists are already initialized by constructor
        return state;
    }

    // ─────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────

    /**
     * Call this after successfully processing a PR.
     *
     * Q: Which list does the response go into?
     * Q: Which counts increase?
     *    - processedCount? (YES - we handled one more PR)
     *    - successCount?   (YES - it succeeded)
     *    - failureCount?   (NO - it didn't fail)
     */
    public void recordSuccess(ReviewResponse response) {
        // TODO: Add to results list
        // TODO: Increment the right counts (hint: TWO counts change)
    }

    /**
     * Call this after a PR fails to process.
     *
     * Q: Same logic as recordSuccess - which list and which counts?
     */
    public void recordFailure(PRFailure failure) {
        // TODO: Add to failures list
        // TODO: Increment the right counts
    }
}
```

**Why does `processedCount` increase in BOTH success and failure?**

Think about the loop that uses this:
```java
for (int i = state.processedCount; i < allPRs.size(); i++) {
    // process PR at index i
}
```

`processedCount` means "how many PRs have we handled" - whether they succeeded OR failed. This tells the next continuation where to resume from. If PR #47 failed, we still don't want to retry it automatically - we move to #48.

---

### Step 3: BatchRequest - The Input

**Purpose:** Simple container for the list of PRs to process.

**Create the file:** `solution/temporal/model/BatchRequest.java`

```java
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

    // The actual PRs to process
    public List<ReviewRequest> pullRequests;

    // Q: Why have a batchId?
    // A: Useful for logging, tracking, and finding this batch in the UI
    public String batchId;

    // No-arg constructor required for Temporal serialization
    public BatchRequest() {}

    public BatchRequest(List<ReviewRequest> pullRequests, String batchId) {
        this.pullRequests = pullRequests;
        this.batchId = batchId;
    }

    /**
     * Q: Why have this helper method?
     * A: Null-safe way to get the count. Avoids NullPointerException.
     */
    public int getTotalCount() {
        return pullRequests != null ? pullRequests.size() : 0;
    }
}
```

---

### Step 4: BatchProgress - For Live Queries

**Purpose:** Let users check progress while the batch is running via `@QueryMethod`.

**Create the file:** `solution/temporal/model/BatchProgress.java`

```java
package solution.temporal.model;

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

    /**
     * Q: Why a factory method instead of exposing BatchState directly?
     * A: Encapsulation! We calculate derived fields (like percentComplete)
     *    and can change BatchState's internals without breaking the query API.
     *
     * TODO: Implement this method
     *
     * Hints:
     *   - percentComplete = (processedCount * 100.0) / totalCount
     *   - Watch out for division by zero if totalCount is 0!
     *   - status could be "PROCESSING" if currentPrTitle is set, else "IN_PROGRESS"
     */
    public static BatchProgress from(BatchState state, int totalCount,
                                      String currentPrTitle, long currentChunkDurationMs) {
        BatchProgress progress = new BatchProgress();
        // TODO: Copy fields from state
        // TODO: Calculate percentComplete (careful: division by zero!)
        // TODO: Set status based on whether we're done
        return progress;
    }
}
```

---

### Step 5: BatchResult - The Final Output

**Purpose:** What the workflow returns when ALL PRs are processed.

**Create the file:** `solution/temporal/model/BatchResult.java`

```java
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
        // TODO: Get totalCount from request
        // TODO: Copy counts from state
        // TODO: continuationCount = state.chunkIndex (each chunk after the first is a continuation)
        // TODO: totalDurationMs = state.accumulatedDurationMs + finalChunkDurationMs
        // TODO: Copy timestamps and lists
        return result;
    }
}
```

---

## Part 4: The Workflow Interface

**Create the file:** `solution/temporal/workflow/BatchPRReviewWorkflow.java`

```java
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
```

---

## Part 5: The Workflow Implementation

**Create the file:** `solution/temporal/workflow/BatchPRReviewWorkflowImpl.java`

This is the longest file. Take it section by section.

### Section A: Setup and Configuration

```java
package solution.temporal.workflow;

import exercise.model.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import solution.temporal.activity.*;
import solution.temporal.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class BatchPRReviewWorkflowImpl implements BatchPRReviewWorkflow {

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * When to trigger continue-as-new.
     *
     * Q: Why 4000 and not 49000 (just under the limit)?
     * A: Safety margin! You want to continue-as-new WELL before hitting the limit.
     *    Also, fewer events = faster replay if the worker restarts.
     *
     * Q: What if you set this too LOW (like 100)?
     * A: Too many continuations. More overhead, harder to debug.
     *
     * Q: What if you set this too HIGH (like 45000)?
     * A: Risk of hitting the hard limit if there's unexpected overhead.
     */
    private static final int HISTORY_THRESHOLD = 4000;

    /**
     * Activity options - same pattern as Exercise 901.
     *
     * Q: Why do we configure retry here and not in the activity?
     * A: Separation of concerns. The activity does the work.
     *    The workflow decides HOW to call it (timeout, retries, etc.)
     */
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();
```

### Section B: Activity Stubs

```java
    // ═══════════════════════════════════════════════════════════════
    // ACTIVITY STUBS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Q: Why do we create stubs as instance fields?
     * A: Workflow code must be deterministic. Creating stubs in methods
     *    could cause issues during replay.
     *
     * TODO: Create the three activity stubs (same pattern as Exercise 901)
     *
     * Hint:
     *   private final CodeQualityActivity codeQualityActivity =
     *       Workflow.newActivityStub(CodeQualityActivity.class, ACTIVITY_OPTIONS);
     */

    // private final CodeQualityActivity codeQualityActivity = ???
    // private final TestQualityActivity testQualityActivity = ???
    // private final SecurityQualityActivity securityQualityActivity = ???
```

### Section C: Workflow State for Queries

```java
    // ═══════════════════════════════════════════════════════════════
    // WORKFLOW STATE (for query method)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Q: Why do we need these fields if we have BatchState?
     * A: The query method needs access to current state.
     *    These are "windows" into what's happening right now.
     *
     * Q: Are these persisted across continue-as-new?
     * A: NO! These are local variables. Only BatchState survives.
     *    That's why we copy state to currentState at the start.
     */
    private BatchState currentState;
    private BatchRequest currentRequest;
    private String currentPrTitle;
    private long chunkStartMs;
```

### Section D: The Main Workflow Method

```java
    // ═══════════════════════════════════════════════════════════════
    // MAIN WORKFLOW METHOD
    // ═══════════════════════════════════════════════════════════════

    @Override
    public BatchResult processBatch(BatchRequest request, BatchState state) {

        // ─────────────────────────────────────────────────────────
        // STEP 1: Initialize or resume state
        // ─────────────────────────────────────────────────────────

        /**
         * Q: Why check if state is null?
         * A: First run: state is null (caller passes null)
         *    Continuation: state has data from previous run
         */
        if (state == null) {
            // FIRST RUN - create fresh state
            // Q: Why use Workflow.currentTimeMillis() instead of System.currentTimeMillis()?
            // A: Determinism! System time changes on replay. Workflow time is consistent.
            String startedAt = Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString();
            state = BatchState.initial(startedAt);
            System.out.println("Starting batch: " + request.getTotalCount() + " PRs");
        } else {
            // CONTINUATION - resuming from previous run
            System.out.println("Continuing batch at chunk #" + (state.chunkIndex + 1));
            System.out.println("Already processed: " + state.processedCount);
        }

        // Store for query access
        this.currentState = state;
        this.currentRequest = request;
        this.chunkStartMs = Workflow.currentTimeMillis();

        // ─────────────────────────────────────────────────────────
        // STEP 2: The main processing loop
        // ─────────────────────────────────────────────────────────

        List<ReviewRequest> allPRs = request.pullRequests;

        /**
         * Q: Why start at state.processedCount instead of 0?
         * A: To skip PRs we already processed in previous runs!
         *
         * Example:
         *   Run 1 processes PRs 0-24, then continues-as-new with processedCount=25
         *   Run 2 starts loop at i=25, skipping the first 25 PRs
         */
        for (int i = state.processedCount; i < allPRs.size(); i++) {

            ReviewRequest pr = allPRs.get(i);
            currentPrTitle = pr.prTitle;  // For query visibility

            System.out.println("[PR " + (i + 1) + "/" + allPRs.size() + "] " + pr.prTitle);

            try {
                // TODO: Process the PR through all 3 agents
                // Hint: Create a helper method processSinglePR(pr) that returns ReviewResponse
                // ReviewResponse response = processSinglePR(pr);
                // state.recordSuccess(response);

            } catch (Exception e) {
                // Q: Why catch and continue instead of letting it fail?
                // A: One bad PR shouldn't stop the whole batch!

                // TODO: Record the failure
                // String failedAt = Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString();
                // PRFailure failure = new PRFailure(i, pr.prTitle, e.getMessage(), failedAt);
                // state.recordFailure(failure);

                System.out.println("  FAILED: " + e.getMessage());
            }

            // ─────────────────────────────────────────────────────
            // STEP 3: Check if we need to continue-as-new
            // ─────────────────────────────────────────────────────

            if (shouldContinueAsNew()) {
                System.out.println("\n>>> History threshold reached. Passing the baton...");

                /**
                 * CRITICAL: Update state BEFORE calling continueAsNew!
                 *
                 * Q: What happens if you forget to update accumulatedDurationMs?
                 * A: You lose this chunk's time from the total.
                 *
                 * Q: What happens if you forget to increment chunkIndex?
                 * A: Your continuation count will be wrong.
                 */
                long chunkDurationMs = Workflow.currentTimeMillis() - chunkStartMs;
                state.accumulatedDurationMs += chunkDurationMs;
                state.chunkIndex++;

                // ═══════════════════════════════════════════════════
                //           THE MAGIC LINE - Pass the baton!
                // ═══════════════════════════════════════════════════
                Workflow.continueAsNew(request, state);

                // ⚠️⚠️⚠️ NOTHING BELOW THIS LINE EVER EXECUTES ⚠️⚠️⚠️
                //
                // The workflow has ALREADY restarted with fresh history.
                // This code is unreachable. If you put important logic here,
                // it will be SILENTLY IGNORED.
                //
                // Common mistake: Putting cleanup code after continueAsNew()
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 4: All PRs processed - build final result
        // ─────────────────────────────────────────────────────────

        /**
         * Q: When does this code run?
         * A: Only on the LAST continuation, when all PRs are done.
         *    Previous runs exit via continueAsNew() above.
         */

        currentPrTitle = null;  // Clear for query - we're done
        long finalChunkDurationMs = Workflow.currentTimeMillis() - chunkStartMs;
        String completedAt = Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString();

        // TODO: Build and return the final result
        // return BatchResult.from(state, request, completedAt, finalChunkDurationMs);

        System.out.println("\n=== BATCH COMPLETE ===");
        return null; // TODO: Replace with actual result
    }
```

### Section E: Query Method

```java
    // ═══════════════════════════════════════════════════════════════
    // QUERY METHOD
    // ═══════════════════════════════════════════════════════════════

    @Override
    public BatchProgress getProgress() {
        /**
         * Q: What if someone queries before processBatch starts?
         * A: currentState would be null. Handle that gracefully!
         */
        if (currentState == null || currentRequest == null) {
            return new BatchProgress(); // Empty progress
        }

        long currentChunkDurationMs = Workflow.currentTimeMillis() - chunkStartMs;

        // TODO: Return actual progress
        // return BatchProgress.from(currentState, currentRequest.getTotalCount(),
        //                           currentPrTitle, currentChunkDurationMs);
        return new BatchProgress();
    }
```

### Section F: Helper Methods

```java
    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Should we pass the baton to a new workflow run?
     */
    private boolean shouldContinueAsNew() {
        /**
         * Q: What is isContinueAsNewSuggested()?
         * A: Temporal's internal recommendation based on history size.
         *    It considers factors we might not know about.
         */
        if (Workflow.getInfo().isContinueAsNewSuggested()) {
            return true;
        }

        /**
         * Q: Why have our own threshold too?
         * A: Defense in depth. Don't rely solely on Temporal's suggestion.
         */
        return Workflow.getInfo().getHistoryLength() > HISTORY_THRESHOLD;
    }

    /**
     * Process a single PR through all 3 agents.
     *
     * TODO: Copy and adapt this from Exercise 901's PRReviewWorkflowImpl
     *
     * Steps:
     * 1. Call codeQualityActivity.analyze(pr)
     * 2. Call testQualityActivity.analyze(pr)
     * 3. Call securityQualityActivity.analyze(pr)
     * 4. Aggregate: BLOCK > REQUEST_CHANGES > APPROVE
     * 5. Build and return ReviewResponse
     *
     * Q: Why not just copy the code inline in the loop?
     * A: Readability and reuse. The main loop stays clean.
     */
    private ReviewResponse processSinglePR(ReviewRequest pr) {
        // TODO: Implement this based on Exercise 901
        return null;
    }

    /**
     * Aggregate agent results into overall recommendation.
     *
     * Logic: If ANY agent says BLOCK → BLOCK
     *        Else if ANY says REQUEST_CHANGES → REQUEST_CHANGES
     *        Else → APPROVE
     */
    private String aggregate(AgentResult... results) {
        for (AgentResult result : results) {
            if ("BLOCK".equals(result.recommendation)) {
                return "BLOCK";
            }
        }
        for (AgentResult result : results) {
            if ("REQUEST_CHANGES".equals(result.recommendation)) {
                return "REQUEST_CHANGES";
            }
        }
        return "APPROVE";
    }
}
```

---

## Part 6: WorkerApp

**Create the file:** `solution/temporal/WorkerApp.java`

```java
package solution.temporal;

import exercise.agents.*;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import solution.temporal.activity.*;
import solution.temporal.workflow.BatchPRReviewWorkflowImpl;

/**
 * The worker process that executes workflows and activities.
 *
 * Q: What happens if no worker is running?
 * A: Workflows will be stuck in "Running" state, waiting for a worker.
 *
 * Q: Can multiple workers run on the same task queue?
 * A: Yes! That's how you scale. Tasks are distributed among workers.
 */
public class WorkerApp {

    private static final String TASK_QUEUE = "batch-pr-review";

    public static void main(String[] args) {
        // 1. Connect to Temporal server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // 3. Register workflow implementation
        // Q: Why register the IMPL class, not the interface?
        // A: Temporal needs to instantiate the class to run workflows.
        worker.registerWorkflowImplementationTypes(BatchPRReviewWorkflowImpl.class);

        // 4. Create agents (business logic)
        // TODO: Create the three agents (same as Exercise 901)
        // CodeQualityAgent codeQualityAgent = new CodeQualityAgent();
        // TestQualityAgent testQualityAgent = ???
        // SecurityAgent securityAgent = ???

        // 5. Register activities with injected agents
        // TODO: Create and register activity implementations
        // worker.registerActivitiesImplementations(
        //     new CodeQualityActivityImpl(codeQualityAgent),
        //     new TestQualityActivityImpl(testQualityAgent),
        //     new SecurityQualityActivityImpl(securityAgent)
        // );

        // 6. Start the worker (blocks forever, listening for tasks)
        factory.start();
        System.out.println("Worker started on queue: " + TASK_QUEUE);
        System.out.println("Press Ctrl+C to stop.");
    }
}
```

---

## Part 7: BatchStarter

**Create the file:** `solution/temporal/BatchStarter.java`

```java
package solution.temporal;

import exercise.model.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import solution.temporal.model.*;
import solution.temporal.workflow.BatchPRReviewWorkflow;

import java.util.*;

/**
 * Client that starts the batch workflow.
 *
 * Q: Why is this separate from the worker?
 * A: Separation of concerns:
 *    - Worker: Executes tasks (long-running process)
 *    - Client: Starts workflows (run once, exit)
 */
public class BatchStarter {

    private static final String TASK_QUEUE = "batch-pr-review";

    /**
     * Adjust this to test continue-as-new behavior:
     *   10 PRs  → Probably no continue-as-new needed
     *   50 PRs  → Might trigger continue-as-new
     *   100 PRs → Will definitely trigger continue-as-new
     */
    private static final int NUM_PRS = 100;

    public static void main(String[] args) {
        // 1. Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Generate sample PRs
        List<ReviewRequest> prs = generateSamplePRs(NUM_PRS);
        String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
        BatchRequest request = new BatchRequest(prs, batchId);

        // 3. Create workflow stub
        String workflowId = "batch-pr-review-" + batchId;

        /**
         * Q: Why set a specific workflowId?
         * A: Makes it easy to find in the UI.
         *    Also prevents duplicate workflows if you run the starter twice.
         */
        BatchPRReviewWorkflow workflow = client.newWorkflowStub(
            BatchPRReviewWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(workflowId)
                .build()
        );

        System.out.println("Starting batch: " + workflowId);
        System.out.println("PRs to process: " + NUM_PRS);
        System.out.println("View at: http://localhost:8233/namespaces/default/workflows/" + workflowId);
        System.out.println();

        /**
         * Q: Why pass null as the second argument?
         * A: This is the FIRST run. No previous state exists.
         *    On continue-as-new, Temporal automatically passes the state.
         */
        BatchResult result = workflow.processBatch(request, null);

        // 5. Print results
        System.out.println("\n=== BATCH COMPLETE ===");
        System.out.println("Batch ID:      " + result.batchId);
        System.out.println("Total PRs:     " + result.totalCount);
        System.out.println("Succeeded:     " + result.successCount);
        System.out.println("Failed:        " + result.failureCount);
        System.out.println("Continuations: " + result.continuationCount);
        System.out.println("Duration:      " + result.totalDurationMs + "ms");
    }

    /**
     * Generate sample PRs for testing.
     */
    private static List<ReviewRequest> generateSamplePRs(int count) {
        List<ReviewRequest> prs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ReviewRequest pr = new ReviewRequest();
            pr.prTitle = "PR-" + (i + 1) + ": Sample change";
            pr.prDescription = "Test PR for batch processing";
            pr.diff = "+ // Change " + i;
            pr.testSummary = new TestSummary(true, 5, 0, 100);
            prs.add(pr);
        }
        return prs;
    }
}
```

---

## Part 8: Testing Your Implementation

### Run Commands

```bash
# Terminal 1: Start Temporal server
temporal server start-dev

# Terminal 2: Start Worker (use DUMMY_MODE to avoid API costs)
cd exercise-905-batch-pr/java
DUMMY_MODE=true mvn compile exec:java@worker

# Terminal 3: Start Batch
mvn compile exec:java@batch
```

### What to Look For in the Temporal UI

1. Go to http://localhost:8233
2. Find your workflow (search for "batch-pr-review")
3. Look for **"ContinueAsNewInitiated"** events in the history
4. Click **"Continue As New Chain"** to see all runs linked together
5. Each run should have ~200-500 events, not thousands

### Query Progress While Running

In a new terminal, while the batch is still processing:

```bash
temporal workflow query \
  --workflow-id=batch-pr-review-YOUR_BATCH_ID \
  --type=getProgress
```

---

## Part 9: Common Mistakes

| Mistake | What You'll See | How to Fix |
|---------|-----------------|------------|
| Lists not initialized in BatchState | NullPointerException on first add | Initialize lists in constructor |
| processedCount not updated | Same PRs processed repeatedly | Call recordSuccess/recordFailure which increment counts |
| Code after continueAsNew() | Code never runs, no error | Move important code BEFORE continueAsNew |
| Starting loop at 0 | PRs processed multiple times | Start at state.processedCount |
| Forgetting chunkIndex++ | continuationCount always 0 | Increment before continueAsNew |
| Using System.currentTimeMillis() | Non-deterministic replay | Use Workflow.currentTimeMillis() |

---

## Part 10: Success Criteria

| Criteria | How to Verify |
|----------|---------------|
| All PRs processed | successCount + failureCount == totalCount |
| Continue-as-new worked | continuationCount >= 1 (for 100 PRs) |
| No duplicates | Each PR title appears once in results |
| Progress queryable | getProgress() returns data mid-run |
| History stays small | Each run < 500 events in Temporal UI |

---

## Stretch Goals

Once the basics work:

1. **Parallel Processing:** Use `Async.function()` to process 5 PRs at once
2. **Pause/Resume:** Add a Signal to pause the batch
3. **External Storage:** Store results in a database instead of carrying them in state
4. **Retry Failed PRs:** Add logic to retry failed PRs at the end

---

## Resources

- [Temporal Continue-As-New Docs](https://docs.temporal.io/develop/java/continue-as-new)
- [Workflow.getInfo() API](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html)
- Exercise 901 source code (your starting point for single-PR logic)

---

**Ready?** Start with `PRFailure.java` (simplest), then `BatchState.java` (most important), then work through the rest in order!
