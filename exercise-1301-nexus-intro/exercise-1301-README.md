# Exercise 1301 — Temporal Nexus Intro (Java)

> **Prerequisite to Exercise 1300.** Build the Nexus mental model here, then tackle the full capstone (async Nexus, signals, parallel execution, and a Next.js dashboard). Think of this as your Nexus driver's license — 1300 is the highway.

---

## Scenario

You work at a digital bank. The **Payments team** processes transactions. Before executing any payment, they must check with the **Compliance team**, who has an AI agent that screens transactions for risk.

Today they do this with a **direct method call** (simulating a REST API call in the real world). When the Compliance service is down, all payments fail instantly. If the check throws an exception, the payment is silently lost. There's no retry. No audit trail. No recovery.

## Quickstart Docs By Temporal

🚀 [Get started in a few mins](https://docs.temporal.io/quickstarts?utm_campaign=awareness-nikolay-advolodkin&utm_medium=code&utm_source=github)

**Your mission:** Replace that fragile direct call with **Temporal Nexus** — durable, type-safe, automatically-retried cross-team communication.

### The 3 Test Transactions

| TXN | Amount | Route | Expected Risk | Expected Outcome |
|-----|--------|-------|---------------|-----------------|
| TXN-A | $250 | US → US | LOW | ✅ Approved, processed |
| TXN-B | $12,000 | US → UK | MEDIUM | ✅ Approved with note |
| TXN-C | $75,000 | US → North Korea | HIGH | 🚫 Declined — `executePayment` never runs |

---

## Step 1: Run the Pre-Temporal Baseline

Before writing anything, see the problem firsthand:

```bash
cd exercise-1301-nexus-intro
export OPENAI_API_KEY=sk-your-key-here
mvn compile exec:java
```

**What you'll observe:**
- TXN-A and TXN-B complete. TXN-C is declined. So far so good.
- But notice what's missing: no retries if compliance throws, no audit trail, no recovery if the process crashes between steps 2 and 3.
- The Payments team directly instantiates `ComplianceAgent` — they've imported Compliance team's code! In real life this means a shared JAR, tight coupling, and a pager alert every time Compliance deploys a breaking change.

**The output will include:**
```
Problems observed:
  1. Direct coupling between Payments and Compliance
  2. No retries on Compliance API failure
  3. No durability — crash = lost transaction
  4. No visibility into which step failed
  5. No audit trail for compliance decisions

  Solution: Temporal Nexus for durable cross-team calls!
```

Read that last line. That's what you're building.

---

## What Is Temporal Nexus?

Think of Nexus as a **phone that never drops calls between teams**.

```
WITHOUT NEXUS                        WITH NEXUS
─────────────────────────────────    ────────────────────────────────────
Payments → REST → Compliance         Payments → Nexus → Compliance

- If Compliance is down: FAIL        - If Compliance is down: RETRY
- If network blips: FAIL             - If network blips: RETRY
- No retry logic: write it yourself  - Retries: automatic
- No audit trail: build it yourself  - Audit trail: event history, free
- Tight coupling: shared JAR         - Decoupled: shared interface only
```

| | REST | Nexus |
|---|------|-------|
| Durability | None | Full (survives crashes) |
| Retries | Write them yourself | Automatic |
| Type safety | OpenAPI/manual | Compile-time |
| Visibility | Grep your logs | Temporal UI shows both sides |
| Audit trail | Build yourself | Built-in event history |
| Team coupling | Shared code or JAR | Shared interface only |

The Payments team's workflow calls `complianceService.checkCompliance(request)` — it looks like a regular Java method call. But under the hood, Temporal routes it to the Compliance team's worker via a named endpoint, handles retries, and records every call in both teams' event histories.

---

## Architecture Overview

```
PAYMENTS TEAM                            COMPLIANCE TEAM
─────────────────────────────────────    ──────────────────────────────────────
PaymentStarter
  └─► PaymentProcessingWorkflow
       │  (task queue: payments-processing)
       │
       ├─ Step 1: validatePayment ──────► PaymentGateway
       │          (activity)               (Payments team owns this)
       │
       ├─ Step 2: ──── Nexus SYNC ──────► ComplianceNexusServiceImpl
       │           checkCompliance()        └─► ComplianceAgent (OpenAI)
       │           (NEW CONCEPT!)           (Compliance team owns this)
       │           returns ComplianceResult
       │
       └─ Step 3: executePayment ────────► PaymentGateway
                  (activity)               (only if compliance.isApproved() == true)
```

### What's New vs Previous Exercises

| Concept | Previous exercises | This exercise |
|---------|-------------------|---------------|
| Workflow + Activities | ✅ | ✅ same pattern |
| Worker setup | ✅ | ✅ + one new registration call |
| Business ID workflow IDs | ✅ Ex 06a | ✅ same pattern |
| Nexus service interface | — | ✅ NEW: shared contract |
| Nexus sync handler | — | ✅ NEW: `@ServiceImpl` |
| Nexus service stub | — | ✅ NEW: `Workflow.newNexusServiceStub()` |

The good news: everything except the three Nexus concepts is muscle memory from Exercise 01-06a. Get those done first.

---

## Breaking Down the Problem

Before writing code, look at `exercise/PaymentProcessingService.java` — the pre-temporal baseline. The same three questions from every exercise apply:

### Question 1: "What is the orchestration logic?"

Strip away the I/O from the `main()` method and what's left is pure decision-making:

```
validate → if invalid, stop
checkCompliance → if not approved, stop
execute payment → done
```

This sequence **IS your workflow.** `PaymentProcessingWorkflowImpl` owns this logic.

### Question 2: "What touches the outside world?"

Scan for anything that could fail due to external factors:

```java
gateway.validatePayment(txn)           // ← calls payment gateway (could fail)
complianceAgent.checkCompliance(req)   // ← calls OpenAI API (could fail, slow)
gateway.executePayment(txn)            // ← calls payment gateway (can randomly throw!)
```

Ask yourself: *"If I unplugged the network cable, would this line break?"* If yes → activity.
Every one of those is an **activity**. They do I/O. They can fail. Temporal retries them automatically.

### Question 3: "Who owns what?"

```
gateway.validatePayment()        → Payments team owns this
complianceAgent.checkCompliance()→ Compliance team owns this ← CROSS-TEAM!
gateway.executePayment()         → Payments team owns this
```

The Payments team shouldn't be directly calling `ComplianceAgent`. That's the Compliance team's code. In the baseline they're tightly coupled — Payments imports Compliance's class directly.

With Nexus, the Compliance team exposes a **service contract** (`ComplianceNexusService` interface). The Payments team calls it like a remote API. Neither team imports the other's implementation.

### Putting It Together

```
PRE-TEMPORAL CODE                         TEMPORAL EQUIVALENT
─────────────────────────────────────────────────────────────────────
gateway.validatePayment(txn)              → PaymentActivity (local activity)
complianceAgent.checkCompliance(compReq) → Nexus SYNC call → inline handler
gateway.executePayment(txn)              → PaymentActivity (local activity)
```

> **Golden rule:** Workflow = `if/else` logic. Activity = anything that talks to the outside world. Nexus = how teams talk to each other without tight coupling.

---

## Follow the Data

Before writing code, trace one transaction end-to-end. Let's follow **TXN-B** ($12,000, US → UK) — the medium-risk path that touches every hop.

> **Interactive version:** Open [`ui/trace.html`](ui/trace.html) in your browser — click each hop to see the exact Java objects that flow in and out, switch transactions to see how TXN-C stops at Hop 3, and use the **Reverse Lookup** tab to trace every `PaymentResult` field back to its source.

### Hop-by-Hop Trace (TXN-B)

**Hop 1: PaymentStarter → PaymentProcessingWorkflow.processPayment()**
```
Input:  PaymentRequest("TXN-B", 12000.00, "USD", "US", "UK",
                       "International consulting fee", "ACC-003", "ACC-004")
What:   Starter creates a workflow stub with ID "payment-TXN-B", calls stub.processPayment(txn)
Output: Workflow is now running on task queue "payments-processing"
        │
        ▼
```

**Hop 2: PaymentActivity.validatePayment()**
```
Input:  PaymentRequest (same object)
What:   Activity delegates to PaymentGateway.validatePayment() — checks amount > 0, accounts non-null
Output: true
        │
        ▼
```

**Hop 3: ── NEXUS SYNC ──► ComplianceNexusServiceImpl.checkCompliance()**
```
Input:  ComplianceRequest("TXN-B", 12000.00, "US", "UK", "International consulting fee")
What:   Nexus sync handler runs inline — ComplianceAgent calls OpenAI to assess risk
Output: ComplianceResult("TXN-B", approved=true, riskLevel="MEDIUM",
                         "International transfer above $10K; approved with AML note")
        │
        ▼
```

**Hop 4: PaymentActivity.executePayment()**
```
Input:  PaymentRequest (original, unchanged)
What:   Activity delegates to PaymentGateway.executePayment() — processes the transfer
        Note: 10% random failure rate → Temporal retries automatically
Output: "CONF-TXN-B-1709234567890"
        │
        ▼
```

**Hop 5: PaymentResult assembled → PaymentStarter**
```
Input:  Values from all hops
Output: PaymentResult(success=true, "TXN-B", "COMPLETED", "MEDIUM",
                      "International transfer above $10K...", "CONF-TXN-B-...", null)
```

For **TXN-C** (HIGH risk, North Korea), Hop 3 returns `approved=false` and the workflow returns `DECLINED_COMPLIANCE` without ever calling `executePayment`. Hop 4 and 5 are skipped entirely.

### Reverse Lookup: Where Does Each PaymentResult Field Come From?

```
PaymentResult field             ← Where it comes from
──────────────────────────────  ─────────────────────────────────────────────
success (true)                  ← reached Step 3 without exceptions
transactionId ("TXN-B")         ← request.getTransactionId() (original input)
status ("COMPLETED")            ← hardcoded in the success path
riskLevel ("MEDIUM")            ← compliance.getRiskLevel() (from Nexus call, Hop 3)
explanation ("International…")  ← compliance.getExplanation() (from Nexus call, Hop 3)
confirmationNumber ("CONF-…")   ← paymentActivity.executePayment() return value (Hop 4)
error (null)                    ← no exceptions thrown
```

---

## What's Already Provided (don't modify these)

| File | What it is |
|------|-----------|
| `shared/nexus/ComplianceNexusService.java` | Nexus service interface — the shared contract |
| `compliance/ComplianceAgent.java` | OpenAI compliance agent (given, use as-is) |
| `compliance/domain/ComplianceRequest.java` | Input POJO for compliance check |
| `compliance/domain/ComplianceResult.java` | Output POJO from compliance check |
| `payments/PaymentGateway.java` | Simulated gateway with 10% failure rate |
| `payments/domain/PaymentRequest.java` | Payment input POJO |
| `payments/domain/PaymentResult.java` | Payment output POJO |
| `payments/temporal/PaymentProcessingWorkflow.java` | Workflow interface |
| `payments/temporal/activity/PaymentActivity.java` | Activity interface |

You implement the **6 files** with `TODO` comments. Follow the phases below in order.

---

## Step-by-Step Implementation

### Phase 1: Activity (Warm-Up — Same Pattern as Exercise 01)

Start here to get your hands warm. Activities are thin wrappers — all they do is make existing business logic retryable by Temporal.

**File 1: `payments/temporal/activity/PaymentActivityImpl.java`**

The `PaymentGateway` already exists and contains all the logic. Your activity is just the bridge:

```java
public class PaymentActivityImpl implements PaymentActivity {

    private final PaymentGateway gateway;

    public PaymentActivityImpl(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean validatePayment(PaymentRequest request) {
        return gateway.validatePayment(request);
    }

    @Override
    public String executePayment(PaymentRequest request) {
        return gateway.executePayment(request);  // returns "CONF-TXN-B-..."
    }
}
```

> **Why so simple?** Business logic lives in `PaymentGateway`. The activity is just the Temporal wrapper that makes those calls retryable, observable, and timeout-able. The gateway doesn't know about Temporal. The activity doesn't know about the business rules. Clean separation.

> **`executePayment` has a 10% failure rate** — `PaymentGateway` randomly throws a `RuntimeException`. Without Temporal, you'd write a retry loop. With activities, Temporal handles it automatically based on the `RetryOptions` you configure in the workflow. You don't write any retry logic here.

<details>
<summary>🧠 Quick check — Phase 1</summary>

**Q: Why delegate to `PaymentGateway` instead of putting the logic directly in the activity?**

<details><summary>Answer</summary>

`PaymentGateway` is a plain Java class with no knowledge of Temporal. Keeping business logic there means it can be tested, reused, and changed without touching Temporal wiring. The activity is just the bridge that makes the gateway call retryable and observable.

</details>

**Q: `executePayment` has a 10% failure rate. Where do you configure how many times Temporal retries it?**

<details><summary>Answer</summary>

In the `RetryOptions` inside `ActivityOptions` in the *workflow* — not in the activity itself. The activity just throws. Temporal catches it and retries based on whatever policy the workflow configured when creating the activity stub.

</details>

</details>

---

### Phase 2: The Nexus Handler — The New Concept

This is the heart of the exercise. The compliance check has to cross team boundaries. Nexus is how that happens.

**File 2: `compliance/temporal/ComplianceNexusServiceImpl.java`**

Think of this class as a **REST controller for your team's service**:

```
ComplianceNexusService (interface)   = your OpenAPI spec
ComplianceNexusServiceImpl (this)    = your Spring @RestController
ComplianceAgent                      = your actual business logic
```

The Payments team sends a request → this handler receives it → delegates to `ComplianceAgent` → returns the result.

Two new annotations to know:

| Annotation | Where it goes | What it means |
|-----------|---------------|---------------|
| `@ServiceImpl(service = ComplianceNexusService.class)` | On the class | "This class handles Nexus requests for that interface" |
| `@OperationImpl` | On each handler method | "This method handles the operation with the matching name" |

⚠️ **The method name must match the interface.** The interface has `checkCompliance()` → your handler method is also named `checkCompliance()`. Temporal matches them by name.

**The SYNC handler pattern** — for quick operations that return immediately:

```java
@ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {

    private final ComplianceAgent agent;

    public ComplianceNexusServiceImpl(ComplianceAgent agent) {
        this.agent = agent;
    }

    @OperationImpl
    public OperationHandler<ComplianceRequest, ComplianceResult> checkCompliance() {
        return WorkflowClientOperationHandlers.sync(
            (context, details, client, input) -> agent.checkCompliance(input)
        );
    }
}
```

The lambda receives four things:
- `context` — Nexus operation context (headers, deadline, etc.)
- `details` — operation metadata (operation name, etc.)
- `client` — a `WorkflowClient` (not needed for sync operations, but it's there)
- `input` — the `ComplianceRequest` that the Payments team sent → pass this to `agent.checkCompliance()`

> **Sync vs Async — when to use which?**
> - **Sync** (`WorkflowClientOperationHandlers.sync`): Use for fast operations. Runs inline, returns a result immediately. Think: quick AI call, validation, lookup.
> - **Async** (`WorkflowClientOperationHandlers.fromWorkflowMethod`): Use for long-running operations. Starts a *whole new workflow* on your side. The caller gets a handle to track it. Think: multi-step fraud investigation, document processing pipeline.
>
> In Exercise 1300, you'll implement both. Here we start with sync only.

<details>
<summary>🧠 Quick check — Phase 2</summary>

**Q: What's the difference between a sync and an async Nexus handler?**

<details><summary>Answer</summary>

A **sync** handler runs inline and returns a result immediately — no new Temporal workflow is started. Good for fast operations like an AI lookup.

An **async** handler starts a *whole new workflow* on the Compliance side and gives the caller a handle to track it. Good for long-running operations that need their own event history. You'll implement async in Exercise 1300.

</details>

**Q: Your handler method is named `checkCompliance()`. What happens if you accidentally name it `handleCompliance()` instead?**

<details><summary>Answer</summary>

Temporal matches handler methods to `@Operation` interface methods by name. A mismatch means the operation has no handler registered — callers get an error at runtime. The method name must be identical to the one in `ComplianceNexusService`.

</details>

</details>

---

### Phase 3: The Compliance Worker — One New Line

**File 3: `compliance/temporal/ComplianceWorkerApp.java`**

This is the standard **CRAWL** worker pattern from previous exercises, with one new call.

#### The CRAWL Pattern — Every Worker Uses This

> **"Workers CRAWL before they run."**

| Step | What you do | API |
|------|------------|-----|
| **C** — Connect | Connect to Temporal server | `WorkflowServiceStubs.newLocalServiceStubs()` → `WorkflowClient.newInstance(service)` |
| **R** — Register | Create the worker factory and worker | `WorkerFactory.newInstance(client)` → `factory.newWorker("compliance-risk")` |
| **A** — Activities | Register activity implementations | *(none in this worker — 1301's compliance handler is sync, no activities needed)* |
| **W** — Wire | Register Nexus service handler ← **NEW** | `worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(agent))` |
| **L** — Launch | Start polling | `factory.start()` |

```java
public static void main(String[] args) {
    // C — Connect
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    // R — Register
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("compliance-risk");  // matches Nexus endpoint target!

    // W — Wire Nexus handler (the new call — everything else is familiar)
    ComplianceAgent agent = new ComplianceAgent();
    worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(agent));

    // L — Launch
    factory.start();
    System.out.println("Compliance Worker started on compliance-risk");
    System.out.println("Waiting for Nexus requests from Payments team...");
}
```

> **The one new thing:** `registerNexusServiceImplementation()`. Compare with activity registration:
> ```java
> worker.registerActivitiesImplementations(new MyActivity());  // familiar
> worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(...));  // new!
> ```
> Without this call, the worker ignores all incoming Nexus requests. With it, it routes them to your `@ServiceImpl` handler.

> **Why `"compliance-risk"` as the task queue?** That's the `--target-task-queue` you'll set when creating the Nexus endpoint via CLI. These strings must match exactly — they're how Temporal finds your worker.

<details>
<summary>🧠 Quick check — Phase 3</summary>

**Q: What happens if you forget `registerNexusServiceImplementation()` in the Compliance worker?**

<details><summary>Answer</summary>

The worker starts without errors but silently ignores all incoming Nexus requests. The Payments workflow will hang (or time out) waiting for a compliance response that never comes. No exception is thrown — the worker just doesn't know it's supposed to handle Nexus calls.

</details>

**Q: The task queue is `"compliance-risk"`. Where else must this exact string appear, and what breaks if they don't match?**

<details><summary>Answer</summary>

It must match the `--target-task-queue` argument you used in the `temporal operator nexus endpoint create` CLI command. If they don't match, Temporal has no route from the endpoint to the worker — Nexus calls will fail with "endpoint not found" or just never be delivered.

</details>

</details>

---

### Phase 4: The Payment Workflow — Orchestrating the 3 Steps

**File 4: `payments/temporal/PaymentProcessingWorkflowImpl.java`**

This is the main orchestrator. It coordinates all three steps without doing any of the actual work itself. Three things to set up as class fields:

#### 4a. Activity Options + Activity Stub (Exercise 01 pattern)

```java
private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))  // how long can one attempt take?
        .setRetryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(1))  // wait 1s before first retry
                .setBackoffCoefficient(2)                   // double the wait each time
                .setMaximumAttempts(3)                      // give up after 3 tries
                .build())
        .build();

private final PaymentActivity paymentActivity =
        Workflow.newActivityStub(PaymentActivity.class, ACTIVITY_OPTIONS);
```

This is identical to every previous exercise. The gateway's 10% failure rate means `executePayment` will occasionally throw — these retry options handle that automatically.

#### 4b. Nexus Service Stub — The New Concept

```java
private final ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
        ComplianceNexusService.class,
        NexusServiceOptions.newBuilder()
                .setOperationOptions(NexusOperationOptions.newBuilder()
                        .setScheduleToCloseTimeout(Duration.ofMinutes(2))
                        .build())
                .build());
```

> **Metaphor:** Think of this like creating an HTTP client, but durable:
> ```java
> // Old way (fragile):
> new HttpClient("http://compliance-service/api/check");
>
> // New way (durable):
> Workflow.newNexusServiceStub(ComplianceNexusService.class, options);
> ```
> Both give you an object you call methods on. But the Nexus stub never drops calls, retries automatically, and records every interaction in the event history.

> **Notice:** The stub doesn't know the endpoint name (`"compliance-endpoint"`). That's configured in `PaymentsWorkerApp`. This separation keeps the workflow portable — same workflow code can run against dev, staging, or prod endpoints by changing worker config.

#### 4c. The `processPayment()` method — 3 steps

Here's what goes where, with every field mapped back to its source:

```java
@Override
public PaymentResult processPayment(PaymentRequest request) {
    Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
            .info("Workflow started for: " + request.getTransactionId());

    try {
        // ════════════════════════════════════════════════════
        // Step 1: Validate payment (local activity)
        // ════════════════════════════════════════════════════
        boolean valid = paymentActivity.validatePayment(request);
        if (!valid) {
            return new PaymentResult(false, request.getTransactionId(), "REJECTED",
                    null, null, null, "Validation failed");
        }

        // ════════════════════════════════════════════════════
        // Step 2: Check compliance via Nexus (the new step!)
        // ════════════════════════════════════════════════════
        ComplianceRequest compReq = new ComplianceRequest(
                request.getTransactionId(),
                request.getAmount(),
                request.getSenderCountry(),
                request.getReceiverCountry(),
                request.getDescription());

        // This LOOKS like a local method call.
        // Under the hood: Temporal routes it to the Compliance worker via Nexus.
        ComplianceResult compliance = complianceService.checkCompliance(compReq);

        Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                .info("Compliance: " + compliance.getRiskLevel() + " | approved=" + compliance.isApproved());

        if (!compliance.isApproved()) {
            return new PaymentResult(false, request.getTransactionId(), "DECLINED_COMPLIANCE",
                    compliance.getRiskLevel(),
                    compliance.getExplanation(),
                    null,
                    "Declined: " + compliance.getExplanation());
        }

        // ════════════════════════════════════════════════════
        // Step 3: Execute payment (local activity — only if approved)
        // ════════════════════════════════════════════════════
        String confirmationNumber = paymentActivity.executePayment(request);

        return new PaymentResult(true, request.getTransactionId(), "COMPLETED",
                compliance.getRiskLevel(),
                compliance.getExplanation(),
                confirmationNumber,
                null);

    } catch (Exception e) {
        Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
                .error("Workflow failed: " + e.getMessage());
        return new PaymentResult(false, request.getTransactionId(), "FAILED",
                null, null, null, e.getMessage());
    }
}
```

> ⚠️ **Use `Workflow.getLogger()`, NOT `System.out.println()`.**
> Workflows are replayed when a worker restarts after a crash. On replay, every line of workflow code runs again. `println` will fire on every replay, cluttering your logs with duplicates. `Workflow.getLogger()` is replay-safe — it only logs during the first execution.

> **What about TXN-C?** For `$75,000 → North Korea`, `compliance.isApproved()` returns `false`. The workflow returns `DECLINED_COMPLIANCE` immediately. `executePayment` is never called. The Temporal UI will show the workflow completing with status DECLINED after only 2 steps — no Step 3 in the event history.

#### Data Flow Cheat Sheet

```
PaymentRequest (input)
    │
    ├─► Step 1: paymentActivity.validatePayment(request)
    │            → boolean valid
    │
    ├─► Step 2: new ComplianceRequest(txnId, amount, senderCountry, receiverCountry, description)
    │            complianceService.checkCompliance(compReq)
    │            → ComplianceResult compliance
    │               └── compliance.isApproved()       ← gate for Step 3
    │               └── compliance.getRiskLevel()     ← goes in PaymentResult
    │               └── compliance.getExplanation()   ← goes in PaymentResult
    │
    └─► Step 3: paymentActivity.executePayment(request)  (only if approved)
                 → String confirmationNumber
                    └── goes in PaymentResult
```

<details>
<summary>🧠 Quick check — Phase 4</summary>

**Q: Why must you use `Workflow.getLogger()` instead of `System.out.println()` in workflow code?**

<details><summary>Answer</summary>

Workflows are replayed every time a worker restarts after a crash. During replay, every line of workflow code runs again. `System.out.println()` fires on every replay, flooding your logs with duplicates and making it impossible to tell which execution actually happened. `Workflow.getLogger()` is replay-safe — it suppresses log output during replay.

</details>

**Q: For TXN-C ($75,000 to North Korea), which of the 3 steps appear in the Temporal event history?**

<details><summary>Answer</summary>

Only Steps 1 and 2. The compliance result comes back `approved=false`, so the workflow returns `DECLINED_COMPLIANCE` immediately. `executePayment` (Step 3) is never scheduled — it won't appear in the event history at all. This is one of the things worth looking for in the Temporal UI after you run the starter.

</details>

**Q: Where does `riskLevel` in the final `PaymentResult` come from?**

<details><summary>Answer</summary>

From the `ComplianceResult` returned by the Nexus call in Step 2 — produced by `ComplianceAgent` running on the **Compliance team's worker**. It crosses the Nexus boundary and ends up in a `PaymentResult` owned by the Payments team. Open `ui/trace.html`, go to the Reverse Lookup tab, and see exactly which hop each field comes from.

</details>

</details>

---

### Phase 5: Workers + Starter (Wiring It All Together)

#### The CRAWL Pattern (Reminder)

> **"Workers CRAWL before they run."**

| Step | What | API |
|------|------|-----|
| **C** | Connect | `WorkflowServiceStubs.newLocalServiceStubs()` → `WorkflowClient.newInstance()` |
| **R** | Register | `WorkerFactory.newInstance(client)` → `factory.newWorker(TASK_QUEUE)` |
| **A** | Activities | `worker.registerActivitiesImplementations(new MyImpl(dep))` |
| **W** | Wire | `worker.registerWorkflowImplementationTypes(optionsWithNexus, MyWorkflowImpl.class)` |
| **L** | Launch | `factory.start()` |

**File 5: `payments/temporal/PaymentsWorkerApp.java`**

Standard worker, with one new twist in the **W** step:

```java
public static void main(String[] args) {
    // C — Connect
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("payments-processing");

    // R + W — Register workflow WITH Nexus endpoint mapping
    //
    // The workflow knows WHAT to call (ComplianceNexusService).
    // The worker knows WHERE to find it ("compliance-endpoint").
    // This keeps the workflow portable — change endpoints without touching workflow code.
    worker.registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                    .setNexusServiceOptions(Collections.singletonMap(
                            "ComplianceNexusService",           // interface name (no package)
                            NexusServiceOptions.newBuilder()
                                    .setEndpoint("compliance-endpoint")  // matches the CLI endpoint
                                    .build()))
                    .build(),
            PaymentProcessingWorkflowImpl.class);

    // A — Activities
    PaymentGateway gateway = new PaymentGateway();
    worker.registerActivitiesImplementations(new PaymentActivityImpl(gateway));

    // L — Launch!
    factory.start();
    System.out.println("Payments Worker started | queue: payments-processing");
    System.out.println("Nexus: ComplianceNexusService → compliance-endpoint");
}
```

> **Analogy:** Like setting `compliance.api.url=https://compliance-service` in `application.properties`. The workflow defines WHAT to call. The worker config defines WHERE.

> **Compare with `ComplianceWorkerApp`:**
> - Compliance worker: `registerNexusServiceImplementation(...)` → **receives** incoming Nexus requests
> - Payments worker: `setNexusServiceOptions(...)` → **sends** outgoing Nexus requests
> Handler side vs caller side. Both use "compliance-endpoint" as the name. They match.

---

#### The START Pattern — Every Starter Uses This

> **"Starters START workflows."**

| Step | What | API |
|------|------|-----|
| **S** | Service: Connect to Temporal | `WorkflowServiceStubs.newLocalServiceStubs()` → `WorkflowClient.newInstance()` |
| **T** | Target: Build `WorkflowOptions` | `.setTaskQueue("payments-processing").setWorkflowId("payment-" + txnId)` |
| **A** | Acquire: Create typed stub | `client.newWorkflowStub(PaymentProcessingWorkflow.class, options)` |
| **R** | Run: Fire off the workflow | `stub.processPayment(txn)` ← blocks until complete |
| **T** | Track: Print result | Log `result.getStatus()`, `result.getRiskLevel()`, etc. |

**File 6: `payments/temporal/PaymentStarter.java`**

In this exercise, transactions run **sequentially** (one at a time). In Exercise 1300 you'll start 5 in parallel with `WorkflowClient.execute()` + `CompletableFuture` — but sequential is the right start here.

```java
public static void main(String[] args) {
    System.out.println("Starting 3 payment transactions via Temporal + Nexus\n");

    PaymentRequest[] transactions = {
        new PaymentRequest("TXN-A", 250.00, "USD", "US", "US",
            "Routine supplier payment", "ACC-001", "ACC-002"),
        new PaymentRequest("TXN-B", 12000.00, "USD", "US", "UK",
            "International consulting fee", "ACC-003", "ACC-004"),
        new PaymentRequest("TXN-C", 75000.00, "USD", "US", "North Korea",
            "Business consulting services", "ACC-005", "ACC-006"),
    };

    // S — Connect to Temporal
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    for (PaymentRequest txn : transactions) {
        // T — Target: business ID workflow ID (not random UUID!)
        String workflowId = "payment-" + txn.getTransactionId(); // "payment-TXN-A"

        // A — Acquire: typed workflow stub
        PaymentProcessingWorkflow wf = client.newWorkflowStub(
                PaymentProcessingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("payments-processing")
                        .setWorkflowId(workflowId)
                        .build());

        System.out.println("Starting: " + workflowId);

        // R — Run: blocks until workflow completes
        PaymentResult result = wf.processPayment(txn);

        // T — Track: print result
        System.out.printf("  → %s | risk=%s | conf=%s%n",
                result.getStatus(),
                result.getRiskLevel(),
                result.getConfirmationNumber() != null ? result.getConfirmationNumber() : "N/A");
        if (result.getExplanation() != null) {
            System.out.println("     " + result.getExplanation());
        }
        System.out.println();
    }

    System.out.println("View in Temporal UI: http://localhost:8233");
    System.out.println("View interactive diagram: open ui/index.html");
}
```

> **Business ID workflow IDs (from Exercise 06a):**
> ```
> ❌ Don't: "payment-" + UUID.randomUUID()    → "payment-a3f2-..." (findable only by luck)
> ✅ Do:    "payment-" + txn.getTransactionId() → "payment-TXN-A" (findable instantly)
> ```
> Open the Temporal UI and search for `payment-TXN-C` — you'll find it immediately. Try that with a UUID.

> **Why does `wf.processPayment(txn)` block?** When you call a workflow method on a stub returned by `client.newWorkflowStub()`, it starts the workflow AND blocks the calling thread until it completes. That's the synchronous calling pattern — simple for sequential workloads.

<details>
<summary>🧠 Quick check — Phase 5</summary>

**Q: What's the advantage of `"payment-TXN-A"` as a workflow ID over `"payment-" + UUID.randomUUID()`?**

<details><summary>Answer</summary>

You can find it in the Temporal UI instantly by searching for the transaction ID — no guessing. It's also idempotent: if you re-run the starter with the same transaction, Temporal rejects the duplicate instead of starting a second workflow. With a UUID, every run creates a new workflow with no connection to the business entity it represents.

</details>

**Q: The workflow uses a `ComplianceNexusService` stub but doesn't mention `"compliance-endpoint"` anywhere. Where is that string configured, and why not in the workflow?**

<details><summary>Answer</summary>

It's configured in `PaymentsWorkerApp` via `NexusServiceOptions.setEndpoint("compliance-endpoint")` at workflow registration time. Keeping it out of the workflow means the same workflow code can point at different endpoints in dev, staging, and production just by changing the worker config — no code changes to the workflow needed.

</details>

**Q: `ComplianceWorkerApp` calls `registerNexusServiceImplementation()`. `PaymentsWorkerApp` sets `NexusServiceOptions`. What's the difference in what each does?**

<details><summary>Answer</summary>

`registerNexusServiceImplementation()` is on the **handler side** — it tells the Compliance worker "I can receive and handle Nexus requests." `NexusServiceOptions` is on the **caller side** — it tells the Payments worker "when this workflow makes a Nexus call, route it to this endpoint." One receives, one sends.

</details>

</details>

---

## How to Run

### Prerequisites

```bash
# Start Temporal server
temporal server start-dev

# Set your OpenAI API key
export OPENAI_API_KEY=sk-your-key-here

# Create the Nexus endpoint (one-time setup — do this BEFORE starting workers)
temporal operator nexus endpoint create \
  --name compliance-endpoint \
  --target-namespace default \
  --target-task-queue compliance-risk
```

> **What does the endpoint create command do?** It tells Temporal: "When a workflow sends a Nexus request to `compliance-endpoint`, route it to the `compliance-risk` task queue." Your Compliance worker listens on that task queue. This is the glue that connects the two teams' workers.

### Running (4 Terminals)

```bash
# Terminal 1: already running — temporal server start-dev

# Terminal 2: Compliance worker (handles incoming Nexus requests)
cd exercise-1301-nexus-intro
mvn compile exec:java@compliance-worker

# Terminal 3: Payments worker (runs workflows + sends Nexus requests)
mvn compile exec:java@payments-worker

# Terminal 4: Run the 3 transactions
mvn compile exec:java@starter
```

### Expected Output (Terminal 4)

```
Starting 3 payment transactions via Temporal + Nexus

Starting: payment-TXN-A
  → COMPLETED | risk=LOW | conf=CONF-TXN-A-1709234567890
     Routine domestic transfer, no regulatory concerns.

Starting: payment-TXN-B
  → COMPLETED | risk=MEDIUM | conf=CONF-TXN-B-1709234589123
     International transfer above $10K; approved with AML note.

Starting: payment-TXN-C
  → DECLINED_COMPLIANCE | risk=HIGH | conf=N/A
     Destination country is OFAC-sanctioned. Transaction blocked.

View in Temporal UI: http://localhost:8233
```

### Optional: Interactive Diagram

```bash
open ui/index.html
```

No install needed. Choose TXN-A/B/C, step through the flow, toggle REST vs Nexus to see why the old approach fails, click any component to learn what it does.

---

## What to Look For in the Temporal UI

After running the starter, open `http://localhost:8233`:

1. **3 payment workflows**: `payment-TXN-A`, `payment-TXN-B`, `payment-TXN-C`
2. Click `payment-TXN-A` → **Event History** shows Step 1 (validatePayment), Step 2 (Nexus call), Step 3 (executePayment) with inputs, outputs, and timing for each
3. Click `payment-TXN-C` → See the workflow completing after only 2 steps — `executePayment` is absent from the event history because compliance blocked it
4. Look for the **Nexus** section in the event history — this shows the cross-team compliance call as its own linked operation
5. **Kill the Compliance worker** (Ctrl+C on Terminal 2), restart the starter, then bring the Compliance worker back. Watch the pending Nexus call resume automatically — no lost transactions.

---

## Common Mistakes

| Symptom | Cause | Fix |
|---------|-------|-----|
| `OPENAI_API_KEY not set` | Missing env var | `export OPENAI_API_KEY=sk-...` |
| `Nexus endpoint not found` | Endpoint not created | Run the `temporal operator nexus endpoint create` command |
| Workflows stuck `Running` | Worker isn't started or crashed on startup | Check Terminal 2/3 for stack traces |
| `UnsupportedOperationException: TODO` | That file isn't implemented yet | Find and fill in the TODO in that file |
| Handler not receiving calls | Task queue mismatch | Compliance worker must use `"compliance-risk"` — matches endpoint target |
| `System.out.println` fires repeatedly | Placed in workflow code | Move to activity, or use `Workflow.getLogger()` |
| `@OperationImpl` method not matching | Method name differs from interface | Handler method must be `checkCompliance()` — exact match |

---

## Key Concepts Recap

| Concept | What it means |
|---------|---------------|
| `@Service` / `@Operation` | Define the cross-team API contract (shared interface — both teams import this) |
| `@ServiceImpl` / `@OperationImpl` | Implement the handler on the Compliance side |
| `WorkflowClientOperationHandlers.sync()` | Handle Nexus call inline — runs fast, returns immediately |
| `Workflow.newNexusServiceStub()` | Create a durable "client" for the cross-team service |
| `registerNexusServiceImplementation()` | Tell the Compliance worker to handle incoming Nexus requests |
| `setNexusServiceOptions()` in worker registration | Tell the Payments worker which endpoint to use |
| `Workflow.getLogger()` | Replay-safe logging — never use `System.out.println()` in workflow code |

---

## What's Saved for Exercise 1300

You now have the Nexus mental model. Exercise 1300 adds:

| Feature | 1301 | 1300 |
|---------|:----:|:----:|
| Sync Nexus (checkCompliance) | ✅ | ✅ |
| Async Nexus (starts a full workflow) | — | ✅ |
| Human approval via Signals | — | ✅ |
| Two LLM agents | — | ✅ |
| Parallel execution (5 transactions) | — | ✅ |
| Full Next.js dashboard | — | ✅ |

The pattern you just built — `@ServiceImpl`, `Workflow.newNexusServiceStub()`, `registerNexusServiceImplementation()` — is the same in 1300. You won't be surprised by any of it.
