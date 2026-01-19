# Exercise 905: Batch Processing + Continue-as-New

## Instructor's Guide

---

### The Story: Why This Matters

**Imagine you're a librarian** who needs to catalog 10,000 books. You could write every single action in one massive logbook:

```
9:00 AM - Picked up Book #1
9:01 AM - Checked spine condition
9:02 AM - Recorded ISBN
9:03 AM - Shelved Book #1
9:04 AM - Picked up Book #2
... (40,000 more entries)
```

After a few thousand entries, your logbook becomes:
- **Impossibly heavy** to carry around
- **Slow to find** where you left off if interrupted
- **At risk of running out of pages**

**Continue-as-New is like starting a fresh logbook** every 500 books, but carrying a small sticky note that says: "Completed books 1-500. Results in filing cabinet drawer #1."

---

### The Problem We're Solving

In Exercise 901, we built a PR review system that processes **one PR** with 3 AI agents. Each PR creates ~11 events in Temporal's history.

**Now scale that to 100+ PRs:**

| Scale | Events | Problem |
|-------|--------|---------|
| 1 PR | ~11 events | No problem |
| 100 PRs | ~1,100 events | Getting heavy |
| 1,000 PRs | ~11,000 events | Replay slows down |
| 10,000 PRs | ~110,000 events | Exceeds 50k limit! |

**Temporal's event history is like RAM** - it's fast but finite. Continue-as-New is like "saving to disk and clearing RAM."

---

### The Architecture (The "Relay Race" Pattern)

```
+------------------------------------------------------------------+
|                    BATCH PR REVIEW WORKFLOW                       |
+------------------------------------------------------------------+
|                                                                   |
|  Runner 1 (Fresh History)          Runner 2 (Fresh History)      |
|  +---------------------+           +---------------------+        |
|  | Process PRs 1-20    |  baton    | Process PRs 21-40   |        |
|  | -----------------> | --------> | -----------------> | ...     |
|  | ~220 events         |  (state)  | ~220 events         |        |
|  +---------------------+           +---------------------+        |
|                                                                   |
|  The "baton" contains:                                            |
|  - How many PRs processed so far                                  |
|  - Which chunk we're on                                           |
|  - Any failures encountered                                       |
|  - Accumulated results (or storage reference)                     |
|                                                                   |
+------------------------------------------------------------------+
```

**Key insight:** Each "runner" is the same workflow, but with a **fresh event history**. The workflow ID stays the same - Temporal links the chain together automatically.

---

### What You'll Build

**File Structure:**
```
exercise-905-batch-pr/
├── exercise-905-README.md          <- This file
└── java/
    ├── pom.xml                     <- Copy from 901, update artifact name
    └── src/main/java/
        ├── exercise/model/         <- Copy from 901 (ReviewRequest, etc.)
        └── solution/temporal/
            ├── model/
            │   ├── BatchRequest.java       <- YOU CREATE
            │   ├── BatchState.java         <- YOU CREATE
            │   ├── BatchResult.java        <- YOU CREATE
            │   ├── BatchProgress.java      <- YOU CREATE
            │   └── PRFailure.java          <- YOU CREATE
            ├── activity/                   <- Copy from 901
            ├── workflow/
            │   ├── BatchPRReviewWorkflow.java      <- YOU CREATE
            │   └── BatchPRReviewWorkflowImpl.java  <- YOU CREATE
            ├── WorkerApp.java              <- YOU CREATE
            └── BatchStarter.java           <- YOU CREATE
```

**5 New Model Classes:**

| Class | Purpose | Metaphor |
|-------|---------|----------|
| `BatchRequest` | Input: list of PRs to review | The stack of documents to process |
| `BatchState` | The "baton" passed between continuations | The sticky note with your progress |
| `BatchResult` | Final output with statistics | The completion report |
| `BatchProgress` | Real-time status (for queries) | The progress bar |
| `PRFailure` | Track which PRs failed | The "problem pile" |

---

### Implementation Challenges

#### Challenge 1: Create BatchState (The "Baton")

**Your task:** Create a class that holds everything needed to resume processing after continue-as-new.

**Hints:**
- What information would you need if the workflow restarted right now?
- How will you know which PRs are already done?
- How will you track total time across continuations?

**Fields to consider:**
```java
public class BatchState {
    // What goes here? Think about:
    // - Progress tracking
    // - Results storage
    // - Error tracking
    // - Timing information
}
```

---

#### Challenge 2: Implement the Continue-as-New Check

**Your task:** Write a method that decides when to "pass the baton."

**The key API:**
```java
WorkflowInfo info = Workflow.getInfo();
info.isContinueAsNewSuggested()  // Temporal's recommendation
info.getHistoryLength()          // Current event count
```

**Question to answer:** At what event count should you trigger continue-as-new? (Hint: stay well under 50,000)

---

#### Challenge 3: The Main Workflow Loop

**Your task:** Write the processing loop that:
1. Processes PRs one at a time (reuse logic from Exercise 901)
2. Updates BatchState after each PR
3. Checks if continue-as-new is needed
4. Calls `Workflow.continueAsNew(...)` when appropriate

**Critical gotcha:** What happens to code AFTER `Workflow.continueAsNew()`? (Answer: it never runs!)

---

#### Challenge 4: Query Methods

**Your task:** Implement `@QueryMethod` so users can check progress while the batch runs.

**APIs to use:**
```java
@QueryMethod
public BatchProgress getProgress() {
    // Return current state
}
```

**Test with:**
```bash
temporal workflow query \
  --workflow-id=YOUR_WORKFLOW_ID \
  --query-type=getProgress
```

---

### The Core Pattern (Reveal After You Try)

<details>
<summary>Click to reveal the continue-as-new pattern</summary>

```java
// The "Should I pass the baton?" check
private boolean shouldContinueAsNew() {
    WorkflowInfo info = Workflow.getInfo();

    // Temporal whispers: "Your history is getting heavy..."
    if (info.isContinueAsNewSuggested()) {
        return true;
    }

    // Or we hit our own safety limit
    return info.getHistoryLength() > 4000;
}

// The main loop
for (ReviewRequest pr : allPRs) {
    ReviewResponse result = processSinglePR(pr);
    state.processedCount++;

    // Check if it's time to pass the baton
    if (shouldContinueAsNew()) {
        state.chunkIndex++;

        // THE MAGIC LINE
        Workflow.continueAsNew(request, state);

        // Code below NEVER executes!
        // Workflow restarts with fresh history
    }
}
```

</details>

---

### Common Pitfalls & Tips

| Pitfall | Why It Happens | Solution |
|---------|----------------|----------|
| **Forgetting state is lost** | Code after `continueAsNew()` never runs | Put ALL important state in BatchState |
| **Infinite loop** | Not incrementing `processedCount` | Always update state BEFORE continue-as-new |
| **Results disappear** | Not carrying results forward | Store in BatchState or external storage |
| **Timing is wrong** | Each continuation resets time | Accumulate duration in BatchState |
| **Processing same PRs twice** | Not tracking start index | Use `processedCount` to skip completed PRs |

**Pro tip:** Think of `continueAsNew()` like hitting "restart" on a video game level, but you keep your inventory (BatchState).

---

### How to Verify It Works

**Run the exercise:**
```bash
# Terminal 1: Start Temporal
temporal server start-dev

# Terminal 2: Start Worker
cd exercise-905-batch-pr/java
mvn compile exec:java@worker

# Terminal 3: Start Batch
mvn compile exec:java@batch
```

**In the Temporal UI (http://localhost:8233):**

1. Find your workflow by ID
2. Look for **"ContinueAsNewInitiated"** events
3. Click **"Continue As New Chain"** to see all continuations
4. Each continuation should have ~200-300 events (not thousands)
5. Final workflow shows **"Completed"**

**What to observe:**

```
Workflow: batch-pr-review-abc123
├── Run #1: 247 events -> ContinueAsNewInitiated
├── Run #2: 251 events -> ContinueAsNewInitiated
├── Run #3: 238 events -> ContinueAsNewInitiated
├── Run #4: 244 events -> ContinueAsNewInitiated
└── Run #5: 189 events -> Completed
```

---

### Success Criteria

| Criteria | How to Verify |
|----------|---------------|
| All 100 PRs processed | `BatchResult.successCount + failureCount == 100` |
| Multiple continuations | `BatchResult.continuationCount >= 4` |
| No duplicate processing | Each PR appears exactly once in results |
| Progress queryable | `getProgress()` returns data mid-execution |
| History stays small | Each run has < 500 events |

---

### Files to Reference from Exercise 901

```
exercise-901-githubpr/java/src/main/java/
├── solution/temporal/
│   ├── PRReviewWorkflowImpl.java  <- Copy processSinglePR logic
│   ├── *Activity.java             <- Reuse all 3 activity interfaces
│   └── *ActivityImpl.java         <- Reuse all 3 implementations
└── exercise/model/
    ├── ReviewRequest.java         <- Reuse as-is
    ├── ReviewResponse.java        <- Reuse as-is
    └── AgentResult.java           <- Reuse as-is
```

---

### Self-Check Questions

Before you start coding, answer these:

1. **What happens to local variables when `continueAsNew()` is called?**

2. **If processing PR #47 triggers continue-as-new, which PR does the new workflow start with?**

3. **Why can't we just use a regular `while` loop that runs forever?**

4. **What would happen if we forgot to increment `processedCount` before continue-as-new?**

5. **How does the Temporal UI show the relationship between continuations?**

---

### Stretch Goals (After You Nail the Basics)

1. **Parallel Processing:** Process 5 PRs at once using `Async.function()`
2. **External Storage:** Store results in a database instead of carrying them
3. **Pause/Resume:** Add signals to pause the batch mid-execution
4. **Retry Failed PRs:** Re-attempt failed PRs at the end of the batch

---

### Resources

- [Temporal Continue-As-New Docs](https://docs.temporal.io/develop/java/continue-as-new)
- [Workflow.getInfo() API](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html)
- Exercise 901 source code (your starting point)

---

**Now go build it!** Start with `BatchState.java` - it's the heart of the Continue-as-New pattern.
