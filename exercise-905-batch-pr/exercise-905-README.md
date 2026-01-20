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
| 10 PRs | ~110 events | ✅ Fine |
| 100 PRs | ~1,100 events | ⚠️ Getting heavy |
| 1,000 PRs | ~11,000 events | 🔴 Exceeds soft limit |
| 5,000 PRs | ~55,000 events | 💥 Workflow terminated! |

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

## Part 3: Designing Your State Classes

### Step 1: BatchState - The Baton (Most Important!)

This is the heart of Continue-as-New. Ask yourself: **"If my workflow restarted RIGHT NOW, what would I need to continue?"**

**Required fields:**

| Field | Type | Why You Need It |
|-------|------|-----------------|
| `processedCount` | `int` | To know which PR to process next (skip already-done PRs) |
| `successCount` | `int` | Track successful reviews |
| `failureCount` | `int` | Track failed reviews |
| `chunkIndex` | `int` | Which continuation we're on (for debugging/stats) |

**Think about these:**

| Field | Type | Why You Might Need It |
|-------|------|----------------------|
| `accumulatedDurationMs` | `long` | Total time spans multiple runs - each run only knows its own time |
| `startedAt` | `String` | When the batch started (set once, carried forward) |
| `results` | `List<ReviewResponse>` | The actual review results |
| `failures` | `List<PRFailure>` | Info about which PRs failed and why |

**Starter code:**

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

    // TODO: Add fields for tracking progress
    // Hint: How many PRs have we processed?

    // TODO: Add fields for tracking success/failure counts

    // TODO: Add field for which continuation chunk we're on

    // TODO: Add field for accumulated time across all runs

    // TODO: Add field for when the batch started (ISO-8601 string)

    // TODO: Add list to store successful results

    // TODO: Add list to store failure information

    // No-arg constructor required for Temporal serialization
    public BatchState() {
        // Initialize your lists here!
    }

    /**
     * Factory method - creates initial state for a brand new batch.
     * Called only on the FIRST run, not on continuations.
     */
    public static BatchState initial(String startedAt) {
        BatchState state = new BatchState();
        state.startedAt = startedAt;
        // Initialize counts to 0, lists to empty
        return state;
    }

    /**
     * Call this after successfully processing a PR.
     */
    public void recordSuccess(ReviewResponse response) {
        // TODO: Add to results list, increment counts
    }

    /**
     * Call this after a PR fails.
     */
    public void recordFailure(PRFailure failure) {
        // TODO: Add to failures list, increment counts
    }
}
```

### Step 2: BatchRequest - The Input

Simple class holding the list of PRs to process.

```java
package solution.temporal.model;

import exercise.model.ReviewRequest;
import java.util.List;

/**
 * Input to the batch workflow - the stack of PRs to review.
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
```

### Step 3: PRFailure - Tracking Problems

When a PR review fails, capture useful debugging info:

```java
package solution.temporal.model;

/**
 * Information about a PR that failed to review.
 */
public class PRFailure {
    public int prIndex;        // Which PR in the batch (0-indexed)
    public String prTitle;     // The PR title (for human readability)
    public String errorMessage; // What went wrong
    public String failedAt;    // When it failed (ISO-8601)

    public PRFailure() {}

    public PRFailure(int prIndex, String prTitle, String errorMessage, String failedAt) {
        this.prIndex = prIndex;
        this.prTitle = prTitle;
        this.errorMessage = errorMessage;
        this.failedAt = failedAt;
    }
}
```

### Step 4: BatchProgress - For Queries

This lets users check progress while the batch is running.

```java
package solution.temporal.model;

/**
 * Current progress - returned by @QueryMethod.
 */
public class BatchProgress {
    public int totalCount;
    public int processedCount;
    public int successCount;
    public int failureCount;
    public int currentChunk;
    public double percentComplete;
    public String currentPrTitle;  // What's being processed right now?
    public String status;          // "PROCESSING", "COMPLETED", etc.

    public BatchProgress() {}

    // TODO: Add a factory method to build this from BatchState
}
```

### Step 5: BatchResult - The Final Output

What the workflow returns when ALL PRs are processed:

```java
package solution.temporal.model;

import exercise.model.ReviewResponse;
import java.util.List;

/**
 * Final result of the batch processing.
 */
public class BatchResult {
    public String batchId;
    public int totalCount;
    public int successCount;
    public int failureCount;
    public int continuationCount;  // How many continue-as-new calls happened
    public long totalDurationMs;
    public String startedAt;
    public String completedAt;
    public List<ReviewResponse> results;
    public List<PRFailure> failures;

    public BatchResult() {}

    // TODO: Add a factory method to build this from BatchState
}
```

---

## Part 4: The Workflow Implementation

### The Workflow Interface

**Key insight:** The workflow method takes TWO parameters - the request AND the state:

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
     * WHY TWO PARAMETERS?
     * - request: The full list of PRs (same across all continuations)
     * - state: The "baton" (null on first run, populated on continuations)
     *
     * On first call: processBatch(request, null)
     * On continuation: processBatch(request, stateFromPreviousRun)
     */
    @WorkflowMethod
    BatchResult processBatch(BatchRequest request, BatchState state);

    /**
     * Query method - check progress without affecting workflow.
     *
     * Test with:
     * temporal workflow query --workflow-id=YOUR_ID --type=getProgress
     */
    @QueryMethod
    BatchProgress getProgress();
}
```

### The Workflow Implementation - Structure

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
     * WHY 4000?
     * - Hard limit is 50,000 events
     * - We want to stay WELL under that
     * - Lower = more continuations but safer
     * - Higher = fewer continuations but riskier
     */
    private static final int HISTORY_THRESHOLD = 4000;

    // Activity options - same as Exercise 901
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    // ═══════════════════════════════════════════════════════════════
    // ACTIVITY STUBS
    // ═══════════════════════════════════════════════════════════════

    // TODO: Create activity stubs (same pattern as Exercise 901)
    // private final CodeQualityActivity codeQualityActivity = ...
    // private final TestQualityActivity testQualityActivity = ...
    // private final SecurityQualityActivity securityQualityActivity = ...

    // ═══════════════════════════════════════════════════════════════
    // WORKFLOW STATE (for query method)
    // ═══════════════════════════════════════════════════════════════

    // These fields let the query method see current progress
    private BatchState currentState;
    private BatchRequest currentRequest;
    private String currentPrTitle;
    private long chunkStartMs;

    // ═══════════════════════════════════════════════════════════════
    // MAIN WORKFLOW METHOD
    // ═══════════════════════════════════════════════════════════════

    @Override
    public BatchResult processBatch(BatchRequest request, BatchState state) {

        // ─────────────────────────────────────────────────────────
        // STEP 1: Initialize or resume state
        // ─────────────────────────────────────────────────────────

        if (state == null) {
            // FIRST RUN - create fresh state
            String startedAt = Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString();
            state = BatchState.initial(startedAt);
            System.out.println("Starting batch: " + request.getTotalCount() + " PRs");
        } else {
            // CONTINUATION - we're resuming from a previous run
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

        // KEY: Start from processedCount, not 0!
        // This skips PRs we already processed in previous runs.
        for (int i = state.processedCount; i < allPRs.size(); i++) {

            ReviewRequest pr = allPRs.get(i);
            currentPrTitle = pr.prTitle;

            try {
                // TODO: Process single PR (reuse logic from Exercise 901)
                // ReviewResponse response = processSinglePR(pr);
                // state.recordSuccess(response);

            } catch (Exception e) {
                // TODO: Record failure, but keep processing other PRs
                // PRFailure failure = new PRFailure(i, pr.prTitle, e.getMessage(), ...);
                // state.recordFailure(failure);
            }

            // ─────────────────────────────────────────────────────
            // STEP 3: Check if we need to continue-as-new
            // ─────────────────────────────────────────────────────

            if (shouldContinueAsNew()) {
                System.out.println("History threshold reached. Continuing as new...");

                // Update accumulated time before passing the baton
                long chunkDurationMs = Workflow.currentTimeMillis() - chunkStartMs;
                state.accumulatedDurationMs += chunkDurationMs;
                state.chunkIndex++;

                // ═══════════════════════════════════════════════════
                // THE MAGIC LINE - Pass the baton!
                // ═══════════════════════════════════════════════════
                Workflow.continueAsNew(request, state);

                // ⚠️ NOTHING BELOW THIS LINE EVER EXECUTES ⚠️
                // The workflow has already restarted with fresh history.
                // If you put important code here, it will be LOST!
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 4: All PRs processed - build final result
        // ─────────────────────────────────────────────────────────

        currentPrTitle = null;  // Clear for query
        long finalChunkDurationMs = Workflow.currentTimeMillis() - chunkStartMs;
        String completedAt = Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString();

        // TODO: Build and return BatchResult
        // return BatchResult.from(state, request, completedAt, finalChunkDurationMs);
        return null; // Replace with actual result
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERY METHOD
    // ═══════════════════════════════════════════════════════════════

    @Override
    public BatchProgress getProgress() {
        // TODO: Build BatchProgress from current state
        // Return empty progress if state not yet initialized
        return new BatchProgress();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Should we pass the baton to a new workflow run?
     */
    private boolean shouldContinueAsNew() {
        // Temporal can tell us if history is getting heavy
        if (Workflow.getInfo().isContinueAsNewSuggested()) {
            return true;
        }

        // Or we hit our own safety threshold
        return Workflow.getInfo().getHistoryLength() > HISTORY_THRESHOLD;
    }

    /**
     * Process a single PR through all 3 agents.
     * Copy and adapt this from Exercise 901's PRReviewWorkflowImpl.
     */
    private ReviewResponse processSinglePR(ReviewRequest pr) {
        // TODO: Call the 3 activities sequentially
        // TODO: Aggregate results (BLOCK > REQUEST_CHANGES > APPROVE)
        // TODO: Return ReviewResponse
        return null;
    }
}
```

---

## Part 5: WorkerApp and BatchStarter

### WorkerApp.java

Same pattern as Exercise 901, but with the batch workflow:

```java
package solution.temporal;

import exercise.agents.*;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import solution.temporal.activity.*;
import solution.temporal.workflow.BatchPRReviewWorkflowImpl;

public class WorkerApp {

    private static final String TASK_QUEUE = "batch-pr-review";

    public static void main(String[] args) {
        // 1. Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // 3. Register workflow
        worker.registerWorkflowImplementationTypes(BatchPRReviewWorkflowImpl.class);

        // 4. Create agents and register activities
        // TODO: Same as Exercise 901

        // 5. Start
        factory.start();
        System.out.println("Worker started on queue: " + TASK_QUEUE);
    }
}
```

### BatchStarter.java

Starts the workflow with a batch of sample PRs:

```java
package solution.temporal;

import exercise.model.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import solution.temporal.model.*;
import solution.temporal.workflow.BatchPRReviewWorkflow;

import java.util.*;

public class BatchStarter {

    private static final String TASK_QUEUE = "batch-pr-review";
    private static final int NUM_PRS = 100;  // Adjust to test continue-as-new

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

        // 4. Start workflow - FIRST run gets null state
        BatchResult result = workflow.processBatch(request, null);

        // 5. Print results
        System.out.println("\n=== BATCH COMPLETE ===");
        System.out.println("Processed: " + result.totalCount);
        System.out.println("Succeeded: " + result.successCount);
        System.out.println("Failed: " + result.failureCount);
        System.out.println("Continuations: " + result.continuationCount);
    }

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

## Part 6: Testing Your Implementation

### Run the Exercise

```bash
# Terminal 1: Start Temporal
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
4. Click the **"Continue As New Chain"** link to see all runs linked together
5. Each run should have ~200-500 events, not thousands

### Query Progress Mid-Execution

While the batch is running:

```bash
temporal workflow query \
  --workflow-id=batch-pr-review-YOUR_BATCH_ID \
  --type=getProgress
```

---

## Part 7: Self-Check Questions

Answer these before looking at the hints:

1. **Why does `processBatch` take two parameters instead of just the request?**

2. **What would happen if you put code after `Workflow.continueAsNew()`?**

3. **Why do we start the loop at `state.processedCount` instead of 0?**

4. **Why do we accumulate duration across runs instead of just timing the last run?**

5. **What would happen if BatchState wasn't serializable (e.g., contained a Thread or InputStream)?**

<details>
<summary>Click to see answers</summary>

1. The `state` parameter lets us pass the "baton" on continuations. First call uses `null`, continuations pass the state from the previous run.

2. That code would **never execute**. `continueAsNew()` immediately ends the current run.

3. To skip PRs we already processed in previous runs. Otherwise we'd process PR #1 over and over!

4. Because `Workflow.currentTimeMillis()` resets on each continuation. We need to sum up all the chunks.

5. The workflow would fail with a serialization error. BatchState must be plain data (strings, numbers, lists, maps).

</details>

---

## Common Mistakes to Avoid

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Forgetting to increment `processedCount` | Infinite loop processing same PRs | Update state BEFORE `continueAsNew()` |
| Putting logic after `continueAsNew()` | Code never runs | Move important code BEFORE the call |
| Not initializing state on first run | NullPointerException | Check `if (state == null)` and create initial state |
| Starting loop at 0 instead of `processedCount` | PRs processed multiple times | Use `for (int i = state.processedCount; ...)` |
| Storing non-serializable data in BatchState | Serialization errors | Only use primitives, Strings, Lists, Maps |

---

## Success Criteria

| Criteria | How to Verify |
|----------|---------------|
| All PRs processed | `successCount + failureCount == totalCount` |
| Multiple continuations | `continuationCount >= 1` (for 100 PRs) |
| No duplicates | Each PR in results exactly once |
| Progress queryable | `getProgress()` returns data mid-run |
| History stays small | Each run < 500 events in UI |

---

## Stretch Goals

Once the basics work:

1. **Parallel Processing:** Use `Async.function()` to process 5 PRs simultaneously
2. **Pause/Resume:** Add a Signal to pause batch mid-execution
3. **External Storage:** Store results in a database instead of carrying them in state
4. **Retry Failed:** Re-process failed PRs at the end of the batch

---

## Resources

- [Temporal Continue-As-New Docs](https://docs.temporal.io/develop/java/continue-as-new)
- [Workflow.getInfo() API](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html)
- Exercise 901 source code (your starting point for single-PR logic)

---

**Ready?** Start with `BatchState.java` - it's the foundation everything else builds on!
