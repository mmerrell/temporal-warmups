# Exercise 1302 — Async Nexus: One New Pattern, Infinite New Possibilities

> **Bridge from Exercise 1301:** You know sync Nexus — handlers that run inline and return immediately. One new pattern changes everything: `fromWorkflowMethod()`. The compliance check now starts a real Temporal workflow. You hold the handle. Temporal does the waiting.

---

## Scenario

**The Deep Investigation** — The Compliance team upgraded their pipeline from a 2-second AI call to a 3-phase forensic investigation:

```
Phase 1: OFAC Screening      — check sanctioned entities and countries
Phase 2: Pattern Analysis    — detect structuring, layering, unusual routing
Phase 3: Final Review        — senior analyst sign-off + risk scoring
```

Total time: **15 to 60 seconds** per transaction. The Payments team's sync Nexus calls now timeout. Transactions are being lost. The on-call engineer just opened a ticket:

> *"Compliance is breaking us again. TXN-BETA timed out at 5 seconds but the investigation took 35. By the time compliance finished, our workflow was already dead."*

Your job: upgrade the integration from sync to async. The Compliance handler will **start** a `ComplianceInvestigationWorkflow` instead of running inline. The Payments workflow will hold a `NexusOperationHandle<InvestigationResult>` and await it durably.

---

## Quickstart Docs By Temporal

🚀 [Get started in a few mins](https://docs.temporal.io/quickstarts?utm_campaign=awareness-nikolay-advolodkin&utm_medium=code&utm_source=github)

---

## Working in This Exercise

```
exercise-1302-async-nexus/
├── exercise/    ← Work here. All TODO files are inside.
├── solution/    ← Reference implementation. Peek only after genuinely trying.
└── ui/          ← Interactive diagrams. Open in browser directly.
    ├── async-nexus-flow.html   ← Primary diagram: toggle sync/async, kill-worker demo
    └── sync-vs-async.html      ← Side-by-side comparison
```

**All checkpoint commands run from `exercise/`:**
```bash
cd exercise-1302-async-nexus/exercise
```

---

## Checkpoint 0 — See the Problem First

Before writing a single line, run the baseline to **feel** the problem:

**Terminal 1:**
```bash
temporal server start-dev
```

**Terminal 2:**
```bash
cd exercise-1302-async-nexus/exercise
mvn compile exec:java
```

**You will see:**
```
╔══════════════════════════════════════════════════════════╗
║   CHECKPOINT 0: The Sync Timeout Problem                ║
║   Payments team calling slow Compliance pipeline        ║
╚══════════════════════════════════════════════════════════╝

  Calling compliance team for TXN-ALPHA...
  Timeout: 5 seconds
  Compliance pipeline takes: ~30 seconds

[SlowComplianceAgent] Starting compliance check for TXN-ALPHA
[SlowComplianceAgent] Phase 1/3: OFAC screening (this takes a while...)

╔══════════════════════════════════════════════════════════╗
║  ERROR: java.util.concurrent.TimeoutException           ║
║  Transaction TXN-ALPHA: LOST                            ║
║  Retries: 0                                             ║
║  Audit trail: none                                      ║
║  On-call engineer: paged                                ║
╚══════════════════════════════════════════════════════════╝
```

**What this means:** A sync call to a slow pipeline is a ticking time bomb. The compliance investigation ran perfectly — it just took longer than the caller expected. Without async Nexus, the payment is permanently lost.

> **Open the interactive diagram to see this visually:**
> `ui/async-nexus-flow.html` → click "Sync Mode (1301) — breaks"

---

## Sync vs Async: The Mental Model

### The Phone Call vs the Ticket System

**Sync Nexus (Exercise 1301):**
> You call Compliance. They answer and you wait on hold for 2 seconds. Fine.
> But now it takes 35 seconds. You're still holding. Your boss calls. You miss it. You're fired.

**Async Nexus (Exercise 1302):**
> You open a ticket: "Please investigate TXN-BETA." You get a ticket number.
> You go do other things. When Compliance closes the ticket, you get a notification.
> You resume where you left off — durably, automatically.

The ticket number is the `NexusOperationHandle`. Temporal holds it. You retrieve the result when it's ready.

### Timeline Comparison

```
SYNC (breaks with slow compliance)
────────────────────────────────────────────────────────────────────────

Payments  │▓▓▓ validate │░░░░░░░░░░░ WAITING (blocked) ░░░░░│💥 TIMEOUT│
          │             │                                    │          │
Compliance│             │▓▓▓ Phase 1 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
          0s            2s                                  5s (crash) 30s


ASYNC (works — TXN-BETA at 35s, TXN-GAMMA at 60s, no problem)
────────────────────────────────────────────────────────────────────────

Payments  │▓▓ validate │▓ start+handle │░ suspended (durable, no thread) ░│▓ execute │
          │             │               │                                   │          │
Compliance│             │▓▓ Phase 1 ▓▓▓▓▓▓▓▓ Phase 2 ▓▓▓▓▓▓▓▓▓ Phase 3 ▓▓▓│
          0s            2s            5s                              35s
```

### The API Difference

| | Exercise 1301 (sync) | Exercise 1302 (async) |
|---|---|---|
| **Handler** | `WorkflowClientOperationHandlers.sync(...)` | `WorkflowClientOperationHandlers.fromWorkflowMethod(...)` |
| **Handler starts?** | Runs inline | Starts a Temporal workflow |
| **Caller API** | `result = complianceService.checkCompliance(req)` | `handle = Workflow.startNexusOperation(...)` |
| **Await** | Inline (blocks) | `handle.getResult().get()` (durable suspend) |
| **Survives timeout?** | No | Yes |
| **Survives worker crash?** | No | Yes — resumes from last completed activity |
| **Visible in Temporal UI?** | Handler only | Workflow + 3 activity completions |

---

## Quick Check — Before You Code

> **3 questions before you start. No peeking. Think it through.**

**Q1:** The compliance worker crashes mid-investigation (Phase 2 of 3). The payment workflow is holding a `NexusOperationHandle`. What happens?

- A) The payment workflow crashes too — it was waiting
- B) The investigation restarts from Phase 1 on the next worker
- C) Temporal retries delivery; the investigation resumes from the last completed activity (Phase 3)
- D) The handle expires and the payment fails

**Q2:** What is the difference between `complianceService.investigate(req)` and `Workflow.startNexusOperation(complianceService::investigate, req)`?

- A) No difference — both return InvestigationResult
- B) The first is a sync call (blocks). The second starts async and gives back a handle
- C) The second runs the investigation inline but in a separate thread
- D) The first uses HTTP; the second uses gRPC

**Q3:** In `fromWorkflowMethod(...)`, why does the lambda end with `::investigate`?

- A) It's a method reference — tells Temporal which workflow method to invoke when starting `ComplianceInvestigationWorkflow`
- B) It filters responses to only "investigate" type operations
- C) It's the handler method name — must match the @OperationImpl method
- D) It specifies the activity to run inside the workflow

*(Answers: C, B, A)*

---

## Architecture: What You Are Building

```
┌─────────────────────────────────┐   Nexus    ┌──────────────────────────────────────┐
│       PAYMENTS TEAM             │  Endpoint  │         COMPLIANCE TEAM               │
│   task-queue: payments-process  │◄──────────►│  task-queue: compliance-investigation  │
│                                 │            │                                        │
│  PaymentProcessingWorkflowImpl  │            │  ComplianceNexusServiceImpl            │
│    1. validatePayment()         │  ─── ► ─── │    fromWorkflowMethod()                │
│    2. startNexusOperation()     │            │      starts ▼                          │
│    3. [suspended]               │            │  ComplianceInvestigationWorkflowImpl   │
│    4. handle.getResult().get()  │  ◄── ─ ─── │    activity.investigate(request)       │
│    5. executePayment()          │            │      Phase 1: OFAC screening           │
│                                 │            │      Phase 2: Pattern analysis         │
│  PaymentsWorkerApp              │            │      Phase 3: Final review             │
│  PaymentActivityImpl            │            │  ComplianceWorkerApp                   │
│                                 │            │  ComplianceInvestigationActivityImpl   │
└─────────────────────────────────┘            └──────────────────────────────────────┘
```

### What Is Given vs What You Implement

| File | Status | Est. Time |
|---|---|---|
| `PaymentActivityImpl.java` | **TODO File 1** — warm-up | 5 min |
| `ComplianceInvestigationActivityImpl.java` | **TODO File 2** — same pattern, new domain | 10 min |
| `ComplianceInvestigationWorkflowImpl.java` | **TODO File 3** — workflow orchestration | 10 min |
| `ComplianceNexusServiceImpl.java` | **TODO File 4** — THE key new concept | 20 min |
| `PaymentProcessingWorkflowImpl.java` | **TODO File 5** — using NexusOperationHandle | 20 min |
| `ComplianceWorkerApp.java` | **TODO File 6** — CRAWL extended | 15 min |
| `PaymentsWorkerApp.java` | **TODO File 7** — CRAWL familiar | 10 min |
| All interfaces, domain POJOs, business logic | **GIVEN** — do not modify | — |

---

## Step 1 — `PaymentActivityImpl.java` (TODO File 1)

**Purpose:** Warm-up. Thin wrapper that delegates to `PaymentGateway`. Identical pattern to previous exercises.

Open `exercise/src/main/java/payments/temporal/activity/PaymentActivityImpl.java` and implement the 4 TODOs:

1. Add `private final PaymentGateway gateway;` field
2. Add constructor that accepts `PaymentGateway`
3. `validatePayment()` → `return gateway.validatePayment(request);`
4. `executePayment()` → `return gateway.executePayment(request);`

---

## Step 2 — `ComplianceInvestigationActivityImpl.java` (TODO File 2)

**Purpose:** Same thin-wrapper pattern, new domain. Confirms you understand the pattern before tackling Nexus.

Open `exercise/src/main/java/compliance/temporal/activity/ComplianceInvestigationActivityImpl.java`:

1. Add `private final ComplianceInvestigator investigator;` field
2. Add constructor that accepts `ComplianceInvestigator`
3. `investigate()` → `return investigator.runFullInvestigation(request);`

### Checkpoint 1 — After File 2

```bash
mvn compile exec:java@activity-test
```

**Expected output:**
```
[INV-ALPHA] Phase 1/3: OFAC screening — checking sanctioned entities...
[INV-ALPHA] Phase 1/3: OFAC screening complete.
[INV-ALPHA] Phase 2/3: Pattern analysis — detecting structuring and layering...
[INV-ALPHA] Phase 2/3: Pattern analysis complete.
[INV-ALPHA] Phase 3/3: Final review — senior analyst sign-off...
[INV-ALPHA] Phase 3/3: Final review complete.
Checkpoint 1 PASSED: ComplianceInvestigationActivityImpl delegates correctly.
```

**If you see `FAILED`:** Check that you're calling `investigator.runFullInvestigation(request)` and not returning `null`.

---

## Step 3 — `ComplianceInvestigationWorkflowImpl.java` (TODO File 3)

**Purpose:** Wire the investigation activity inside a workflow. The interface `ComplianceInvestigationWorkflow` is given; you implement the body.

Open `exercise/src/main/java/compliance/temporal/ComplianceInvestigationWorkflowImpl.java`:

**TODO 1 — Define `ACTIVITY_OPTIONS`:**
```java
private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofMinutes(2))
    .setHeartbeatTimeout(Duration.ofSeconds(10))
    .setRetryOptions(RetryOptions.newBuilder()
        .setMaximumAttempts(3)
        .setInitialInterval(Duration.ofSeconds(1))
        .setBackoffCoefficient(2.0)
        .build())
    .build();
```

> **Why heartbeat timeout?** `ComplianceInvestigator` calls `Activity.getExecutionContext().heartbeat()` during each phase. If the activity stops heartbeating (worker crash), Temporal detects it within 10 seconds and reschedules. This is what makes the Heist Test (Checkpoint 4) work correctly.

**TODO 2 — Create activity stub:**
```java
private final ComplianceInvestigationActivity activity =
    Workflow.newActivityStub(ComplianceInvestigationActivity.class, ACTIVITY_OPTIONS);
```

**TODO 3 — Implement `investigate()`:**
```java
return activity.investigate(request);
```

---

## Step 4 — `ComplianceNexusServiceImpl.java` (TODO File 4) — The Core Lesson

**This is the key file.** The entire exercise builds to this moment.

Open `exercise/src/main/java/compliance/temporal/ComplianceNexusServiceImpl.java`.

### What You Did in 1301 (sync)

```java
// Handler runs inline — works for fast operations, breaks for slow ones
return WorkflowClientOperationHandlers.sync(
    (context, details, client, input) ->
        complianceAgent.checkCompliance(input)
);
```

### What You Implement in 1302 (async)

```java
// Handler STARTS a workflow — works for any duration
return WorkflowClientOperationHandlers.fromWorkflowMethod(
    (context, details, client, input) ->
        client.newWorkflowStub(
            ComplianceInvestigationWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("investigation-" + input.getTransactionId())
                .build()
        )::investigate
);
```

**Mental model for `fromWorkflowMethod()`:**
> You are not running the workflow. You are registering **how to start it**. When a Nexus request arrives, Temporal runs your lambda to get a workflow stub + method reference, starts the workflow, and returns an operation token to the caller. The caller doesn't know (or care) that a workflow is running — they just have a handle.

**The `::investigate` method reference — why it matters:**
This tells Temporal which `@WorkflowMethod` to invoke on `ComplianceInvestigationWorkflow`. The method name must exactly match the interface declaration. A mismatch causes a runtime error when the workflow is started.

**Implement the 3 TODOs:**

1. Add `@ServiceImpl(service = ComplianceNexusService.class)` to the class
2. Add `@OperationImpl` to the `investigate()` method
   *(method name must be exactly `investigate` — Temporal matches by name)*
3. Return `WorkflowClientOperationHandlers.fromWorkflowMethod(...)` with:
   - Workflow stub for `ComplianceInvestigationWorkflow.class`
   - `workflowId = "investigation-" + input.getTransactionId()`
   - Method reference: `stub::investigate`

---

## Step 5 — `PaymentProcessingWorkflowImpl.java` (TODO File 5) — The New API

**Purpose:** Use `NexusOperationHandle` on the calling side. The conceptual partner to File 4.

Open `exercise/src/main/java/payments/temporal/PaymentProcessingWorkflowImpl.java`.

The `ACTIVITY_OPTIONS` are already given as a starting point. You implement the stubs and the 5 orchestration steps.

### TODO 1b — Create paymentActivity stub

```java
private final PaymentActivity paymentActivity =
    Workflow.newActivityStub(PaymentActivity.class, ACTIVITY_OPTIONS);
```

### TODO 1c — Create complianceService Nexus stub

```java
private final ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
    ComplianceNexusService.class,
    NexusServiceOptions.newBuilder()
        .setOperationOptions(NexusOperationOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofMinutes(2))
            .build())
        .build());
```

> **Where does `"compliance-endpoint"` come from?**
> Not here — the endpoint name is configured in `PaymentsWorkerApp` via `WorkflowImplementationOptions`. The workflow only knows the *service* (`ComplianceNexusService`). The *worker* knows the *endpoint*. This keeps the workflow portable.

### TODO 2 — Validate payment

```java
boolean valid = paymentActivity.validatePayment(request);
if (!valid) {
    return new PaymentResult(false, request.getTransactionId(),
        "REJECTED", null, null, null, "Validation failed");
}
```

### TODO 3 — Build request and start Nexus operation

```java
InvestigationRequest investigationReq = new InvestigationRequest(
    request.getTransactionId(), request.getAmount(),
    request.getSenderCountry(), request.getReceiverCountry(),
    request.getDescription());

NexusOperationHandle<InvestigationResult> handle =
    Workflow.startNexusOperation(complianceService::investigate, investigationReq);
```

> **`startNexusOperation()` returns immediately.** No blocking. Temporal schedules the Nexus call and returns a handle. The workflow can continue doing other work — or, in this case, immediately await the result.

### TODO 4 — Await result

```java
InvestigationResult investigation = handle.getResult().get();
if (investigation.isBlocked()) {
    return new PaymentResult(false, request.getTransactionId(),
        "BLOCKED_COMPLIANCE", investigation.getRiskLevel(),
        investigation.getSummary(), null, null);
}
```

> **`handle.getResult().get()` is a durable await.** The workflow suspends here. No thread is blocked. If the Payments worker crashes, Temporal replays the workflow from history — re-attaches to the in-progress investigation — and waits again. The investigation is not restarted.

### TODO 5 — Execute payment

```java
String confirmation = paymentActivity.executePayment(request);
return new PaymentResult(true, request.getTransactionId(),
    "COMPLETED", investigation.getRiskLevel(),
    investigation.getSummary(), confirmation, null);
```

Wrap all steps in `try/catch` and return a `"FAILED"` result on unexpected error.

---

## Step 6 — `ComplianceWorkerApp.java` (TODO File 6)

**Purpose:** Register the full async compliance infrastructure. Unlike 1301's compliance worker (handler only), this worker also registers the investigation workflow and activity.

Open `exercise/src/main/java/compliance/temporal/ComplianceWorkerApp.java`.

Follow the CRAWL pattern (the comments in the file guide you):

```
C — Connect: WorkflowServiceStubs + WorkflowClient
R — Register workflow: worker.registerWorkflowImplementationTypes(ComplianceInvestigationWorkflowImpl.class)
A — Register activity: worker.registerActivitiesImplementations(new ComplianceInvestigationActivityImpl(new ComplianceInvestigator()))
W — Wire Nexus:        worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl())
L — Launch:            factory.start() + startup banner
```

**Task queue: `"compliance-investigation"`** — must match `--target-task-queue` in the endpoint creation command below.

### Checkpoint 2 — After Files 4 + 6

Create the Nexus endpoint (run once, then it persists):

```bash
temporal operator nexus endpoint create \
  --name compliance-endpoint \
  --target-namespace default \
  --target-task-queue compliance-investigation
```

Start the workers:

```bash
# Terminal 3:
mvn compile exec:java@compliance-worker

# Terminal 4:
mvn compile exec:java@payments-worker
```

**Expected output (Terminal 3):**
```
╔══════════════════════════════════════════════════════════╗
║   Compliance Investigation Worker — ONLINE               ║
║   Task queue: compliance-investigation                   ║
║   Waiting for payment investigation requests...          ║
╚══════════════════════════════════════════════════════════╝
```

**Open Temporal UI** (`http://localhost:8233`) → **Nexus** tab → `compliance-endpoint` should appear with status `HEALTHY`.

If the endpoint shows as unhealthy: verify the task queue in `ComplianceWorkerApp.TASK_QUEUE` matches `--target-task-queue` exactly.

---

## Step 7 — `PaymentsWorkerApp.java` (TODO File 7)

**Purpose:** Wire the payments side. Same CRAWL pattern as Exercise 1301.

Open `exercise/src/main/java/payments/temporal/PaymentsWorkerApp.java`.

Follow the CRAWL pattern:

```
C — Connect
R — Register PaymentProcessingWorkflowImpl with Nexus endpoint mapping:
    WorkflowImplementationOptions.newBuilder()
        .setNexusServiceOptions(Collections.singletonMap(
            "ComplianceNexusService",
            NexusServiceOptions.newBuilder()
                .setEndpoint("compliance-endpoint")
                .build()))
        .build()
A — Register: new PaymentActivityImpl(new PaymentGateway())
L — Launch + banner
```

> **The mapping key `"ComplianceNexusService"`** is the class name (no package prefix). Temporal uses this to find the correct NexusServiceOptions when the workflow calls the service.

### Checkpoint 3 — Full End-to-End

Start the payments worker (if not already running), then in a new terminal:

```bash
# Terminal 5:
mvn compile exec:java@starter
```

**Expected output:**
```
╔══════════════════════════════════════════════════════════╗
║   PAYMENT STARTER — Exercise 1302: Async Nexus          ║
║   Starting 3 transactions (parallel investigations)     ║
╚══════════════════════════════════════════════════════════╝

  Launching: payment-TXN-ALPHA ($3,500 -> Germany)
  Launching: payment-TXN-BETA ($47,500 -> Cayman Islands)
  Launching: payment-TXN-GAMMA ($180,000 -> Iran)

  All 3 workflows started. Waiting for results...

╔══════════════════════════════════════════════════════════╗
║                    PAYMENT RESULTS                      ║
╚══════════════════════════════════════════════════════════╝
  [✓] TXN-ALPHA     | COMPLETED             | Risk: LOW      | Transaction approved...
  [✓] TXN-BETA      | COMPLETED             | Risk: MEDIUM   | Transaction approved with elevated...
  [✗] TXN-GAMMA     | BLOCKED_COMPLIANCE    | Risk: CRITICAL | Transaction blocked: destination...
```

**In Temporal UI you should see 6 workflows:**
- `payment-TXN-ALPHA`, `payment-TXN-BETA`, `payment-TXN-GAMMA` (payment workflows)
- `investigation-TXN-ALPHA`, `investigation-TXN-BETA`, `investigation-TXN-GAMMA` (investigation workflows)

Click any payment workflow → look for `NexusOperationScheduled` and `NexusOperationStarted` events in the event history. Click the linked workflow ID to jump to the investigation workflow.

---

## Temporal UI Walkthrough

### 1. Nexus Tab
Navigate to **Nexus** in the left sidebar. You should see `compliance-endpoint` with:
- Status: HEALTHY
- Target: `default/compliance-investigation`

### 2. Investigation Workflow Event History
Click any `investigation-TXN-*` workflow. You'll see:
- `WorkflowExecutionStarted` — triggered by the Nexus handler
- `ActivityTaskScheduled` / `ActivityTaskStarted` / `ActivityTaskCompleted` for each phase
- `WorkflowExecutionCompleted` — carries the `InvestigationResult`

### 3. Linked Workflows via Nexus Events
Click any `payment-TXN-*` workflow → scroll to the `NexusOperationScheduled` event → expand it. You'll find the operation token. Temporal uses this to link the two workflows across the Nexus boundary.

### 4. TXN-GAMMA Blocked — Full Audit Trail
Even though `investigation-TXN-GAMMA` results in `blocked=true`, the investigation workflow runs all 3 phases and completes normally. The **decision** is in the data; the **execution** is always durable. This is the audit trail you couldn't get with a plain timeout.

---

## The Heist Test — Checkpoint 4

> **Prove durability by killing the worker mid-investigation.**

Run with timestamp-suffixed IDs (so you don't hit ALREADY_EXISTS):

```bash
mvn compile exec:java@starter-rerun
```

Watch for Phase 2 of TXN-BETA:
```
[INV-BETA] Phase 2/3: Pattern analysis — detecting structuring and layering...
```

**Press Ctrl+C** on the compliance worker (Terminal 3). Watch what happens:

1. The payment starter does **not** crash — TXN-BETA is durable
2. In Temporal UI, `investigation-TXN-BETA` shows **Running** (not Failed)
3. `payment-TXN-BETA` shows **Running** (suspended at `handle.getResult().get()`)

**Restart the compliance worker:**
```bash
# Terminal 3 (restart):
mvn compile exec:java@compliance-worker
```

**You will see:**
```
[INV-BETA] Phase 3/3: Final review — senior analyst sign-off...
[INV-BETA] Phase 3/3: Final review complete.
```

Temporal **resumed from Phase 3** — it did not restart the investigation from Phase 1. Phase 1 and Phase 2 completion events were already written to the event log. The payment eventually completes.

> **This is what "durable" means.** A crash mid-operation is a deployment detail, not a failure.

> **Also try the diagram:** `ui/async-nexus-flow.html` → click "Start Animation" → click "Kill Compliance Worker" during Phase 2 → click "Restart Compliance Worker".

---

## Sync vs Async Retrospective

### Side by Side — The Handler

**Exercise 1301 (sync):**
```java
@ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {
    private final ComplianceAgent agent;

    @OperationImpl
    public OperationHandler<ComplianceRequest, ComplianceResult> checkCompliance() {
        return WorkflowClientOperationHandlers.sync(
            (context, details, client, input) ->
                agent.checkCompliance(input)   // ← runs inline, blocks caller
        );
    }
}
```

**Exercise 1302 (async):**
```java
@ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {

    @OperationImpl
    public OperationHandler<InvestigationRequest, InvestigationResult> investigate() {
        return WorkflowClientOperationHandlers.fromWorkflowMethod(
            (context, details, client, input) ->
                client.newWorkflowStub(
                    ComplianceInvestigationWorkflow.class,
                    WorkflowOptions.newBuilder()
                        .setWorkflowId("investigation-" + input.getTransactionId())
                        .build()
                )::investigate   // ← starts workflow, returns token
        );
    }
}
```

**What changed:** `sync()` → `fromWorkflowMethod()`. The caller (`PaymentProcessingWorkflowImpl`) changed from a direct call to `Workflow.startNexusOperation()` + `handle.getResult().get()`. Nothing else changed. The Nexus service *interface* is identical in structure.

### When to Use Each

| Use `sync()` when... | Use `fromWorkflowMethod()` when... |
|---|---|
| Operation completes in < 10 seconds | Operation takes minutes, hours, or has no SLA |
| No worker crash risk during operation | Worker restarts are normal (deploys, scaling) |
| Stateless, inline calculation | Multi-step process with checkpoints |
| No audit trail needed | Full event history required |

---

## What's Next: Exercise 1300

Exercise 1300 builds on everything you've done here:

| Feature | 1302 | 1300 |
|---|---|---|
| Async Nexus | ✓ | ✓ |
| Multiple NexusOperationHandles (parallel) | — | ✓ |
| Signals for human approval | — | ✓ |
| Two LLM agents (GPT + Claude) | — | ✓ |
| Next.js real-time dashboard | — | ✓ |
| 5 transactions in parallel | — | ✓ |

---

## Troubleshooting

### "Nexus endpoint not found" / ComplianceNexusService call fails
- Verify endpoint was created: `temporal operator nexus endpoint list`
- Check that `--target-task-queue compliance-investigation` matches `ComplianceWorkerApp.TASK_QUEUE`
- Check that `--name compliance-endpoint` matches the endpoint name in `PaymentsWorkerApp`

### `OperationHandler` returns `null` for `investigate()`
- You forgot `@OperationImpl` on the method, or
- The method name in `ComplianceNexusServiceImpl` doesn't match the interface (`investigate`)

### `WorkflowExecutionAlreadyStarted`
- Use `mvn compile exec:java@starter-rerun` for re-runs — it uses timestamp-suffixed IDs
- Or restart `temporal server start-dev` to clear state

### Investigation never completes / workflow stuck
- Check that `ComplianceWorkerApp` registers **all three**: workflow, activity, Nexus handler
- Check heartbeat timeout: `setHeartbeatTimeout(Duration.ofSeconds(10))` in `ComplianceInvestigationWorkflowImpl`

### `NullPointerException` in `PaymentProcessingWorkflowImpl`
- Verify activity stub field is initialized (not null)
- Verify Nexus stub field is initialized
- Check that `PaymentActivityImpl` returns real values, not `false`/`null`

### Python version / wrong JDK
- Java 11+ required: `java -version`
- Maven: `mvn -version`

---

> **You did it.** Sync Nexus was a phone call. Async Nexus is a durable ticket system — one that survives crashes, restarts, and long investigations. The callers don't block. The compliance team runs independently. And Temporal keeps the receipt.
