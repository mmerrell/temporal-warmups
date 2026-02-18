# Exercise 1300 - Cross-Team Payment Compliance via Temporal Nexus (Java)

## Scenario

You work at a digital bank where two engineering teams need to communicate reliably:

- **Payments Team** - Processes payment transactions
- **Compliance/Risk Team** - AI-powered fraud detection and transaction categorization

Today they communicate via REST. When the Compliance team's service goes down, ALL payments fail. When a fraud check succeeds but the categorization call fails, the fraud check is wasted. There's no way to pause a payment for human review of high-risk transactions. There's no audit trail showing how cross-team decisions were made.

**Temporal Nexus** solves this by providing a durable, type-safe, observable contract between teams.

| TXN | Amount | Route | Risk Pattern |
|-----|--------|-------|-------------|
| TXN-001 | $250 | US → US | Low risk (rent) |
| TXN-002 | $49,999 | US → Cayman Islands | Suspicious destination |
| TXN-003 | $12.50 | US → US | Low risk (coffee) |
| TXN-004 | $150,000 | Russia → US | Sanctions risk |
| TXN-005 | $9,999 | US → US | Structuring? (just under $10k threshold) |

## Quickstart Docs By Temporal

🚀 [Get started in a few mins](https://docs.temporal.io/quickstarts?utm_campaign=awareness-nikolay-advolodkin&utm_medium=code&utm_source=github)

## Prerequisites

1. **Java 11+ and Maven**
2. **Node.js 18+** (for the dashboard UI)
3. **OpenAI API Key** (for AI-powered fraud detection and categorization):
   ```bash
   export OPENAI_API_KEY=sk-your-key-here
   ```
4. **Temporal Server** (needed later, not for the baseline):
   ```bash
   temporal server start-dev
   ```

## Step 1: Start the Dashboard UI

Start the dashboard first — it works in both pre-Temporal and Temporal modes:

```bash
cd exercise-1300-nexus-payments/ui
npm install && npm run dev
```

Open **http://localhost:3000** — you'll see an empty dashboard.

## Step 2: Run the Pre-Temporal Baseline

In a second terminal:

```bash
cd exercise-1300-nexus-payments
mvn compile exec:java
```

**Watch the dashboard** as transactions are processed in real time. You'll see the orange **"Pre-Temporal Mode"** banner indicating direct REST calls.

Click any transaction to see the 5-step processing pipeline. Notice:
- Steps complete instantly with no durability
- Step 4 (Approval Wait) is always skipped — there's no way to pause for human review
- If any step fails, the whole transaction is lost

**Problems you'll observe:**
1. No retry logic — if fraud detection API fails, the whole payment fails
2. No durability — crash mid-process and you lose everything
3. Tight coupling — Payments team directly instantiates Compliance team's code
4. No human approval — high-risk transactions are auto-processed (dangerous!)
5. No cross-team visibility or audit trail
6. Compliance outage blocks ALL payments

## Your Task

Convert this cross-team communication into **Temporal Nexus** - a durable, type-safe contract between the Payments and Compliance teams.

## What is Temporal Nexus?

Think of Nexus as **durable RPC between Temporal applications**:

```
┌──────────────────┐         ┌──────────────────────┐
│  Payments Team   │  Nexus  │  Compliance Team     │
│                  │ ──────► │                      │
│  PaymentWorkflow │         │  FraudDetection WF   │
│  (caller)        │ ◄────── │  Categorizer Agent   │
│                  │         │  (handler)           │
└──────────────────┘         └──────────────────────┘
```

| | REST | Nexus |
|---|------|-------|
| Durability | None | Full (survives crashes) |
| Retries | Manual | Automatic |
| Type safety | OpenAPI/manual | Compile-time |
| Visibility | Logs only | Temporal UI shows both sides |
| Audit trail | Build yourself | Built-in event history |

## Breaking Down the Problem

### Architecture Overview

```
PaymentStarter
  └── starts PaymentProcessingWorkflow (payments-processing queue)
         ├── validatePayment() ......... local activity
         ├── categorizeTransaction() ... Nexus SYNC call → Compliance team
         ├── screenTransaction() ....... Nexus ASYNC call → FraudDetectionWorkflow
         ├── [if high risk] ............ Workflow.await() for approval Signal
         └── executePayment() .......... local activity
```

### Key Nexus Concepts

#### 1. Nexus Service Interface (shared contract)
Both teams agree on a shared interface - like a proto definition:
```java
public interface ComplianceNexusService {
    RiskScreeningResult screenTransaction(RiskScreeningRequest request);   // async
    TransactionCategory categorizeTransaction(CategoryRequest request);    // sync
}
```

#### 2. Nexus Handler (Compliance team implements)
The handler defines HOW each operation is fulfilled:
- **Async handler** → Starts a workflow (long-running, durable)
- **Sync handler** → Runs inline (quick, returns immediately)

#### 3. Nexus Caller (Payments team calls)
```java
ComplianceNexusService compliance = Workflow.newNexusServiceStub(
    ComplianceNexusService.class,
    NexusServiceOptions.newBuilder()
        .setEndpoint("compliance-endpoint")
        .build());

// Call it like a local method - Nexus handles the rest!
TransactionCategory cat = compliance.categorizeTransaction(request);
```

#### 4. Nexus Endpoint (CLI registration)
```bash
temporal operator nexus endpoint create \
  --name compliance-endpoint \
  --target-namespace default \
  --target-task-queue compliance-risk
```

### Combined Patterns from Previous Exercises

This exercise combines everything you've learned:
- **Workflows + Activities** (Ex 01-04)
- **Signals + Human-in-the-loop** (Ex 06) - approval for high-risk transactions
- **Parallel execution + Business IDs** (Ex 06a) - 5 payments started concurrently

## Step 3: Implement Temporal Nexus (Step-by-Step)

Keep the dashboard running from Step 1 — when you start the Temporal workers later, the UI will automatically switch from the orange "Pre-Temporal Mode" to the green **"Temporal Nexus Mode"**.

### How to Decompose: From Pre-Temporal to Temporal

Before writing code, let's look at `PaymentProcessingService.java` and think about what goes where. The decomposition process is always the same three questions:

#### Question 1: "What is the orchestration logic?"

Look at the `for` loop in the pre-temporal code. Strip away the I/O and what's left is pure **decision-making**:

```
validate → if invalid, stop
screen for fraud → check risk level
categorize → record the category
if high risk → need approval (but can't wait today!)
execute payment
```

This sequence of decisions **IS your workflow**. It doesn't call APIs directly — it coordinates. It says "do this, then based on the result, do that." That's `PaymentProcessingWorkflowImpl`.

#### Question 2: "What touches the outside world?"

Scan the pre-temporal code for anything that could fail due to external factors:

```java
gateway.validatePayment(txn)          // ← calls payment gateway (could fail)
fraudAgent.screenTransaction(req)     // ← calls OpenAI API (could fail, slow)
categorizerAgent.categorize(catReq)   // ← calls OpenAI API (could fail, slow)
gateway.executePayment(txn)           // ← calls payment gateway (could fail)
```

Every one of these is an **activity**. They do I/O. They can fail. They should be retried. Ask yourself: "If I unplugged the network cable, would this line break?" If yes → activity.

#### Question 3: "Who owns what?"

This is the Nexus question. Look at who owns each piece of business logic:

```
gateway.validatePayment()        → Payments team owns this
fraudAgent.screenTransaction()   → Compliance team owns this ← CROSS-TEAM!
categorizerAgent.categorize()    → Compliance team owns this ← CROSS-TEAM!
gateway.executePayment()         → Payments team owns this
```

The Payments team shouldn't directly instantiate `FraudDetectionAgent` or `TransactionCategorizerAgent` — those belong to the Compliance team. In the pre-temporal code, they're tightly coupled. With Nexus, the Compliance team exposes a **service contract**, and the Payments team calls it like a remote API — but with durability, retries, and visibility built in.

#### Putting It Together

Here's how each line of pre-temporal code maps to the Temporal world:

```
PRE-TEMPORAL CODE                         TEMPORAL EQUIVALENT
─────────────────────────────────────────────────────────────────────
for (PaymentRequest txn : transactions)   PaymentStarter starts 5 workflows
                                          (parallel, with business ID)

  gateway.validatePayment(txn)            → PaymentActivity (local activity)
  fraudAgent.screenTransaction(req)       → Nexus ASYNC call → FraudDetectionWorkflow
  categorizerAgent.categorize(catReq)     → Nexus SYNC call → inline handler
  if (riskResult.isRequiresApproval())    → Workflow.await() for Signal (NEW!)
    // can't wait!                           (now we CAN wait — up to 24 hours)
  gateway.executePayment(txn)             → PaymentActivity (local activity)
```

> **The golden rule:** Workflow code is the `if/else` logic. Activity code is anything that talks to the outside world. Nexus is how teams talk to each other without tight coupling.

---

The interfaces and domain classes are already provided. You'll implement the **7 files** marked with `// TODO` comments. Follow the phases below in order — each builds on the last.

### What's Already Provided (don't modify these)

| File | What it is |
|------|-----------|
| `shared/nexus/ComplianceNexusService.java` | Nexus service interface — the shared contract |
| `compliance/temporal/FraudDetectionWorkflow.java` | Workflow interface |
| `compliance/temporal/activity/FraudDetectionActivity.java` | Activity interface |
| `payments/temporal/PaymentProcessingWorkflow.java` | Workflow interface + `@SignalMethod` |
| `payments/temporal/activity/PaymentActivity.java` | Activity interface |
| `compliance/temporal/ComplianceWorkerApp.java` | Compliance worker (fully wired) |
| All `domain/` classes | Request/Response POJOs |

---

### Phase 1: Activities (Warm-up — same pattern as Exercises 01-04)

Start with the simplest files to get back into the groove.

**File 1: `compliance/temporal/activity/FraudDetectionActivityImpl.java`**
- Constructor and field are provided
- Implement `screenTransaction()`: delegate to `fraudAgent.screenTransaction(request)`
- That's it — thin wrapper around the existing business logic

**File 2: `payments/temporal/activity/PaymentActivityImpl.java`**
- Constructor and field are provided
- Implement `validatePayment()`: delegate to `gateway.validatePayment(request)`
- Implement `executePayment()`: delegate to `gateway.executePayment(request)`

> **Checkpoint:** These are pure delegation. If you've done Exercise 01, this is muscle memory.

---

### Phase 2: Compliance Workflow (same pattern as Exercises 01-04)

**File 3: `compliance/temporal/FraudDetectionWorkflowImpl.java`**
- Create an `ActivityOptions` with retry policy (hints in the TODOs)
- Create an activity stub: `Workflow.newActivityStub(FraudDetectionActivity.class, options)`
- In `screenTransaction()`: call the activity and return the result
- Use `Workflow.getLogger()` for logging (NOT `System.out.println`)

> **Checkpoint:** This workflow is called BY Nexus, but you don't need to know that yet. It's just a normal workflow that wraps an activity.

---

### Phase 3: The Nexus Handler (NEW CONCEPT)

This is the heart of the exercise. Take your time here.

**File 4: `compliance/temporal/ComplianceNexusServiceImpl.java`**

Think of this like a **REST controller** — it receives requests from the Payments team and decides HOW to handle them:

```
Payments team calls Nexus          Nexus handler decides what to do
─────────────────────────── ──►  ───────────────────────────────────
screenTransaction(req)            → Start a FraudDetectionWorkflow (ASYNC)
categorizeTransaction(req)        → Run categorization inline (SYNC)
```

**Two handler patterns to implement:**

**ASYNC handler** — `screenTransaction()`:
```java
@OperationImpl
public OperationHandler<RiskScreeningRequest, RiskScreeningResult> screenTransaction() {
    return WorkflowClientOperationHandlers.fromWorkflowMethod(
        (context, details, client, input) ->
            client.newWorkflowStub(
                FraudDetectionWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("fraud-screen-" + input.getTransactionId())
                    .build()
            )::screenTransaction     // ← method reference to the workflow method
    );
}
```

What this does:
1. Payments team calls `screenTransaction()` via Nexus
2. This handler creates a `FraudDetectionWorkflow` stub with a business ID
3. Temporal starts that workflow on the Compliance side
4. The Payments team gets a handle to track progress and wait for the result

**SYNC handler** — `categorizeTransaction()`:
```java
@OperationImpl
public OperationHandler<CategoryRequest, TransactionCategory> categorizeTransaction() {
    return WorkflowClientOperationHandlers.sync(
        (context, details, client, input) -> {
            TransactionCategorizerAgent agent = new TransactionCategorizerAgent();
            return agent.categorize(input);
        });
}
```

Sync is simpler — runs inline, returns immediately. Good for quick operations.

> **When to use which?**
> - **Async** (fromWorkflowMethod): Operation takes time, needs durability, benefits from its own event history. Think: fraud analysis, document processing, approval flows.
> - **Sync**: Quick operation, doesn't need its own workflow. Think: categorization, validation, lookups.

> **Checkpoint:** After this file, the Compliance side is complete. The `ComplianceWorkerApp.java` is already wired to register this handler via `worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl())`.

---

### Phase 4: The Payment Workflow (Nexus CALLER + Signals)

**File 5: `payments/temporal/PaymentProcessingWorkflowImpl.java`**

This is the biggest file — it orchestrates all 5 steps. But each step uses a pattern you already know:

**New concept: Nexus service stub**
```java
private final ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
    ComplianceNexusService.class,
    NexusServiceOptions.newBuilder()
        .setOperationOptions(NexusOperationOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofMinutes(5))
            .build())
        .build());
```

Then use it like a local method:

| Step | Pattern | Code |
|------|---------|------|
| 1. Validate | Activity (Ex 01-04) | `paymentActivity.validatePayment(request)` |
| 2. Categorize | **Nexus SYNC** (NEW) | `complianceService.categorizeTransaction(catReq)` |
| 3. Fraud Screen | **Nexus ASYNC** (NEW) | `Workflow.startNexusOperation(complianceService::screenTransaction, req)` then `handle.getResult().get()` |
| 4. Approval | Signal + await (Ex 06) | `Workflow.await(Duration.ofHours(24), () -> approvalReceived)` |
| 5. Execute | Activity (Ex 01-04) | `paymentActivity.executePayment(request)` |

The sync Nexus call (step 2) looks just like calling a regular method. The async call (step 3) uses `startNexusOperation` to get a handle, then `.getResult().get()` to wait.

> **Checkpoint:** The workflow is complete. Now you need the worker and the starter.

---

### Phase 5: Worker + Starter (wiring it up)

**File 6: `payments/temporal/PaymentsWorkerApp.java`**

Standard worker pattern, but with one new twist — **Nexus endpoint mapping**:

```java
worker.registerWorkflowImplementationTypes(
    WorkflowImplementationOptions.newBuilder()
        .setNexusServiceOptions(Collections.singletonMap(
            "ComplianceNexusService",              // Service interface name
            NexusServiceOptions.newBuilder()
                .setEndpoint("compliance-endpoint") // Matches the CLI endpoint
                .build()))
        .build(),
    PaymentProcessingWorkflowImpl.class);
```

This tells the worker: "When PaymentProcessingWorkflow uses the ComplianceNexusService stub, route those calls to the `compliance-endpoint` Nexus endpoint."

> **Analogy:** Like configuring `compliance.api.url=http://compliance:8080` in a Spring app. The workflow defines WHAT it calls, the worker config defines WHERE.

**File 7: `payments/temporal/PaymentStarter.java`**

Parallel execution pattern from Exercise 06a:
- Loop through 5 transactions
- For each: create a workflow stub with a business ID (`"payment-" + txnId`), start with `WorkflowClient.execute()` to get a `CompletableFuture`
- Wait for all results

> **Checkpoint:** All 7 files are complete. Time to run it!

## How to Run (Temporal Mode)

### Terminal 1: Dashboard UI (already running from Step 1)

### Terminal 2: Temporal Server
```bash
temporal server start-dev
```

### Terminal 3: Create Nexus Endpoint
```bash
temporal operator nexus endpoint create \
  --name compliance-endpoint \
  --target-namespace default \
  --target-task-queue compliance-risk
```

### Terminal 4: Compliance Worker
```bash
cd exercise-1300-nexus-payments
mvn compile exec:java@compliance-worker
```

### Terminal 5: Payments Worker
```bash
cd exercise-1300-nexus-payments
mvn compile exec:java@payments-worker
```

### Terminal 6: Start Payments
```bash
cd exercise-1300-nexus-payments
mvn compile exec:java@starter
```

### Terminal 7: Send Approval Signals (for high-risk transactions)
```bash
# Approve TXN-002 (Cayman Islands transfer)
temporal workflow signal --workflow-id payment-TXN-002 \
  --name approveTransaction \
  --input '{"approved":true,"reviewerName":"Jane","reason":"Verified client identity"}'

# Reject TXN-004 (Russia - sanctions)
temporal workflow signal --workflow-id payment-TXN-004 \
  --name approveTransaction \
  --input '{"approved":false,"reviewerName":"Jane","reason":"Sanctions violation"}'

# Approve TXN-005 (structuring concern)
temporal workflow signal --workflow-id payment-TXN-005 \
  --name approveTransaction \
  --input '{"approved":true,"reviewerName":"Bob","reason":"Recurring deposit verified"}'
```

## What to Observe

### 1. Low-Risk Auto-Complete
TXN-001 ($250 rent) and TXN-003 ($12.50 coffee) should complete automatically - no human approval needed.

### 2. High-Risk Waiting for Approval
TXN-002, TXN-004, TXN-005 should pause and wait for your approval signal.

### 3. Temporal UI (http://localhost:8233)
- Find `payment-TXN-001` - see the Nexus operation calls in the event history
- Find `fraud-screen-TXN-002` - see the Compliance workflow started BY Nexus
- **See linked workflows** - Nexus connects Payment and Compliance workflows visually

### 4. Cross-Team Durability
Kill the Compliance worker (Ctrl+C), then restart it. Pending fraud screenings will resume automatically!

### 5. Next.js Dashboard (already running)
The dashboard at http://localhost:3000 now shows the green **"Temporal Nexus Mode"** banner:

- **Transactions page** (`/`) — Overview of all payment workflows with risk badges and real-time status
- **Flow Visualization** — Click any transaction ID to see the **5-step workflow pipeline**:
  - Step 1: Validate Payment (Payments team)
  - Step 2: Categorize Transaction (Compliance via Nexus - sync)
  - Step 3: Fraud Screening (Compliance via Nexus - async)
  - Step 4: Approval Wait (signal-based human review)
  - Step 5: Execute Payment (Payments team)
  - Each step shows: status (pending/in-progress/completed/failed/waiting), duration, and which team owns it
  - Cross-team steps are visually marked as **Compliance (Nexus)**
  - Expand "Raw Temporal Events" to see the full event history
- **Approvals page** (`/approvals`) — Send approve/reject signals for high-risk transactions without the CLI

**Compare!** Run the pre-Temporal baseline (`mvn compile exec:java`) and the Temporal version side by side. Notice how the dashboard automatically detects which mode is active — orange for pre-Temporal (no retries, no durability) vs green for Temporal Nexus (full observability).

## Real-World Applications

- **Payment processing** - Cross-team compliance checks with audit trails
- **Insurance claims** - Claims team calls Underwriting team via Nexus
- **Order fulfillment** - Orders team calls Inventory + Shipping teams
- **Loan origination** - Applications team calls Credit + KYC teams
- **Any microservice communication** that needs durability and observability

## Hints

<details>
<summary>Hint 1: Nexus Service Interface</summary>

The interface goes in `shared/nexus/` because both teams need it. It defines the contract - what operations are available and their input/output types.
</details>

<details>
<summary>Hint 2: Nexus Handler - Async vs Sync</summary>

- **Async** (WorkflowRunOperation): Use for long-running operations. Starts a workflow. The caller gets a handle to track it.
- **Sync** (OperationHandler.sync): Use for quick operations. Runs inline and returns immediately.

Fraud detection = async (could take minutes). Categorization = sync (quick AI call).
</details>

<details>
<summary>Hint 3: Worker Registration</summary>

Compliance worker needs: `worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl())`

This is the only new registration pattern - everything else is the same as previous exercises.
</details>

<details>
<summary>Hint 4: Nexus Stub in Workflow</summary>

```java
ComplianceNexusService compliance = Workflow.newNexusServiceStub(
    ComplianceNexusService.class,
    NexusServiceOptions.newBuilder()
        .setEndpoint("compliance-endpoint")  // matches CLI endpoint name
        .setOperationOptions(NexusOperationOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofMinutes(5))
            .build())
        .build());
```

Then call it like any other method: `compliance.categorizeTransaction(request)`
</details>
