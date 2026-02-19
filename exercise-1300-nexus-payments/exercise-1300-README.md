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

### Follow the Data

Before diving into code, trace how data flows through the entire system. This follows **TXN-002** ($49,999, US → Cayman Islands) — the high-risk path that touches every hop.

**[View animated data-flow diagram](http://localhost:3000/data-flow.svg)** — watch the orange ball move through all 8 hops (~30s loop).

#### Hop-by-Hop Trace (TXN-002)

**Hop 1: PaymentStarter.main() → PaymentProcessingWorkflow.processPayment()**
```
  Input:  PaymentRequest(TXN-002, $49,999, USD, US → Cayman Islands, "Offshore investment transfer")
  What:   Starter creates workflow stub with ID "payment-TXN-002", calls WorkflowClient.execute()
  Output: Workflow is now running on task queue "payments-processing"
         │
         ▼
```

**Hop 2: PaymentActivity.validatePayment()**
```
  Input:  PaymentRequest (same object passed through)
  What:   Activity delegates to PaymentGateway.validatePayment() — checks amount > 0, accounts exist
  Output: true (payment is valid)
         │
         ▼
```

**Hop 3: ── NEXUS SYNC ──► ComplianceNexusServiceImpl.categorizeTransaction()**
```
  Input:  CategoryRequest(TXN-002, $49,999, "Offshore investment transfer", US, Cayman Islands)
  What:   Nexus sync handler runs inline — TransactionCategorizerAgent calls AI to classify
  Output: TransactionCategory(category="INTL_TRANSFER", subCategory="OFFSHORE", regulatoryFlags=[...])
         │
         ▼
```

**Hop 4: ── NEXUS ASYNC ──► ComplianceNexusServiceImpl.screenTransaction()**
```
  Input:  RiskScreeningRequest(TXN-002, $49,999, US, Cayman Islands, "Offshore investment transfer")
  What:   Nexus async handler starts FraudDetectionWorkflow with ID "fraud-screen-TXN-002"
  Output: NexusOperationHandle — Payments workflow holds a handle, waits for the result
         │
         ▼
```

**Hop 5: FraudDetectionWorkflowImpl → FraudDetectionActivity → FraudDetectionAgent (AI)**
```
  Input:  RiskScreeningRequest (passed through from Hop 4)
  What:   Workflow calls activity, activity delegates to FraudDetectionAgent which calls AI/LLM
  Output: RiskScreeningResult(riskLevel="HIGH", riskScore=0.87, requiresApproval=true, flaggedSanctions=false)
         │
         ▼
```

**Hop 6: Workflow PAUSED ← Signal arrives**
```
  Input:  riskResult.isRequiresApproval() == true → Workflow.await(24h, () -> approvalReceived)
  What:   Workflow is durably paused. Human sends signal via CLI or Approvals UI.
  Signal: ApprovalDecision(approved=true, reviewerName="Jane", reason="Verified client identity")
  Output: approvalReceived=true, approved=true → workflow unblocks
         │
         ▼
```

**Hop 7: PaymentActivity.executePayment()**
```
  Input:  PaymentRequest (original request)
  What:   Activity delegates to PaymentGateway.executePayment() — processes the actual transfer
  Output: "CONF-TXN-002-a1b2c3" (confirmation number)
         │
         ▼
```

**Hop 8: PaymentResult assembled → PaymentStarter**
```
  Input:  All values collected from previous hops
  What:   Workflow constructs PaymentResult and returns it to the CompletableFuture in PaymentStarter
  Output: PaymentResult(true, "TXN-002", "COMPLETED", "HIGH", "INTL_TRANSFER", "CONF-TXN-002-a1b2c3", null)
```

#### Reverse Lookup: Where Does Each PaymentResult Field Come From?

```
PaymentResult field          ← Where it comes from
─────────────────────────    ──────────────────────────────────────────
success (true)               ← reached Step 5 without exceptions
transactionId ("TXN-002")   ← request.getTransactionId() (original input)
status ("COMPLETED")         ← all steps passed (hardcoded in Step 5)
riskLevel ("HIGH")           ← riskResult.getRiskLevel() (Hop 5 — Compliance team)
category ("INTL_TRANSFER")   ← category.getCategory() (Hop 3 — Compliance team)
confirmationNumber           ← paymentActivity.executePayment() return value (Hop 7)
error (null)                 ← no exceptions thrown
```

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

Start with the simplest files to get back into the groove. Activities are thin wrappers — they just bridge Temporal and your existing business logic.

**File 1: `compliance/temporal/activity/FraudDetectionActivityImpl.java`**

The constructor and `fraudAgent` field are already provided. You just need to fill in the method body:

```java
@Override
public RiskScreeningResult screenTransaction(RiskScreeningRequest request) {
    // One line: delegate to the agent and return the result
    return fraudAgent.screenTransaction(request);
}
```

That's it. The `FraudDetectionAgent` already knows how to call OpenAI and return a `RiskScreeningResult`. The activity just makes it retryable by Temporal.

**File 2: `payments/temporal/activity/PaymentActivityImpl.java`**

Same pattern — the constructor and `gateway` field are provided. Two methods, each a one-liner:

```java
@Override
public boolean validatePayment(PaymentRequest request) {
    return gateway.validatePayment(request);
}

@Override
public String executePayment(PaymentRequest request) {
    return gateway.executePayment(request);  // Returns a confirmation number like "CONF-TXN-001-..."
}
```

> **Why are these so simple?** Activities should be thin. The business logic lives in `FraudDetectionAgent` and `PaymentGateway` — those are plain Java classes that don't know about Temporal. The activity is just the bridge that lets Temporal retry, timeout, and track these calls.

> **Checkpoint:** If you've done Exercise 01, this is muscle memory. Two files, a few lines each.

---

### Phase 2: Compliance Workflow (same pattern as Exercises 01-04)

**File 3: `compliance/temporal/FraudDetectionWorkflowImpl.java`**

This workflow wraps the fraud detection activity with retry policies. Three things to set up:

**1. Activity options** (class-level constant):
```java
private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(60))   // OpenAI can be slow
        .setRetryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(2)) // Wait 2s before first retry
                .setBackoffCoefficient(2)                  // Double the wait each retry
                .setMaximumAttempts(5)                      // Give up after 5 tries
                .build())
        .build();
```

**2. Activity stub** (class-level field):
```java
private final FraudDetectionActivity fraudActivity =
        Workflow.newActivityStub(FraudDetectionActivity.class, ACTIVITY_OPTIONS);
```

**3. Method body** — call the activity and return the result:
```java
@Override
public RiskScreeningResult screenTransaction(RiskScreeningRequest request) {
    Workflow.getLogger(FraudDetectionWorkflowImpl.class)
            .info("Fraud detection started for: " + request.getTransactionId());

    RiskScreeningResult result = fraudActivity.screenTransaction(request);

    Workflow.getLogger(FraudDetectionWorkflowImpl.class)
            .info("Result: " + result.getRiskLevel() + " (score: " + result.getRiskScore() + ")");

    return result;
}
```

> **Why `Workflow.getLogger()` instead of `System.out.println()`?** Workflows can be replayed (that's how Temporal recovers from crashes). During replay, `println` would print again, cluttering your logs. `Workflow.getLogger()` is replay-safe — it only logs during the first execution.

> **Checkpoint:** This workflow is called BY Nexus (from Phase 3), but you don't need to know that yet. It's just a normal workflow that wraps an activity. If you've done Exercise 01-04, this is the same pattern.

---

### Phase 3: The Nexus Handler (NEW CONCEPT)

This is the heart of the exercise. Take your time here.

**File 4: `compliance/temporal/ComplianceNexusServiceImpl.java`**

Think of this like a **REST controller** — it receives requests from the Payments team and decides HOW to handle them.

Here's the full sequence of what happens when the Payment workflow makes a Nexus call:

```
PAYMENTS TEAM                                    COMPLIANCE TEAM
PaymentProcessingWorkflow                        ComplianceNexusServiceImpl
┌─────────────────────────┐                      ┌─────────────────────────────┐
│                         │                      │                             │
│  STEP 2 (SYNC)          │                      │  categorizeTransaction()    │
│                         │  ── Nexus call ────► │                             │
│  complianceService      │                      │  Runs inline:               │
│    .categorize(catReq)  │                      │    agent.categorize(input)  │
│                         │  ◄── result ──────── │    returns immediately      │
│  category = result      │  TransactionCategory │  (use this var in Steps 4&5)│
│                         │                      │                             │
│  STEP 3 (ASYNC)         │                      │  screenTransaction()        │
│                         │  ── Nexus call ────► │                             │
│  handle = Workflow       │                      │  Starts NEW workflow:       │
│    .startNexusOperation │  ◄── handle ──────── │  FraudDetectionWorkflow     │
│                         │                      │         │                   │
│                         │                      │         ▼                   │
│  // workflow waits...   │                      │  ┌───────────────────────┐  │
│                         │                      │  │ AI/LLM fraud analysis │  │
│                         │                      │  │ Could take minutes... │  │
│                         │                      │  └───────────┬───────────┘  │
│                         │                      │              │              │
│  riskResult = handle    │  ◄── result ──────── │◄─────────────┘              │
│    .getResult().get()   │  RiskScreeningResult │                             │
│                         │                      │                             │
└─────────────────────────┘                      └─────────────────────────────┘
       Task Queue:                                        Task Queue:
  "payments-processing"                             "compliance-processing"
```

**Key insight:** Both teams run their own workers on separate task queues. Nexus is the bridge between them — like an internal API, but durable.

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

This is the biggest file — it orchestrates all 5 steps. Take it one step at a time. The method signature is:

```java
public PaymentResult processPayment(PaymentRequest request) { ... }
```

Everything you need comes from `request` (the `PaymentRequest` passed in). Here's a quick reference of what's available:

```
request.getTransactionId()    → "TXN-001"
request.getAmount()           → 250.00
request.getDescription()      → "Monthly rent payment"
request.getSenderCountry()    → "US"
request.getReceiverCountry()  → "US"
request.getCurrency()         → "USD"
request.getSenderAccount()    → "ACC-SENDER-001"
request.getReceiverAccount()  → "ACC-RECV-001"
```

#### Setting up the class fields (before the method)

You need three things declared as fields at the top of the class:

**1. Signal state** — instance fields to track the human approval (Exercise 06 pattern):
```java
private boolean approvalReceived = false;
private boolean approved = false;
private String reviewerName = "";
private String approvalReason = "";
```

**2. Activity stub** — for local payment operations (Exercise 01-04 pattern):
```java
private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))
        .setRetryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(1))
                .setBackoffCoefficient(2)
                .setMaximumAttempts(5)
                .build())
        .build();

private final PaymentActivity paymentActivity =
        Workflow.newActivityStub(PaymentActivity.class, ACTIVITY_OPTIONS);
```

**3. Nexus service stub** — to call the Compliance team (NEW):
```java
private final ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
        ComplianceNexusService.class,
        NexusServiceOptions.newBuilder()
                .setOperationOptions(NexusOperationOptions.newBuilder()
                        .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                        .build())
                .build());
```

> **Think of it this way:** The activity stub talks to YOUR team's activities. The Nexus stub talks to the OTHER team's services. Both look like regular method calls.

#### Step-by-step implementation of `processPayment()`

**Step 1: Validate payment** (activity call — you know this)

```java
boolean valid = paymentActivity.validatePayment(request);
if (!valid) {
    return new PaymentResult(false, request.getTransactionId(), "REJECTED",
            null, null, null, "Payment validation failed");
}
```

**Step 2: Categorize via Nexus** (sync call — looks like a regular method)

You need to build a `CategoryRequest` from the `PaymentRequest` values. The constructor is:
```
CategoryRequest(transactionId, amount, description, senderCountry, receiverCountry)
```

So you map from `request`:
```java
CategoryRequest catReq = new CategoryRequest(
        request.getTransactionId(),      // transactionId
        request.getAmount(),             // amount
        request.getDescription(),        // description
        request.getSenderCountry(),      // senderCountry
        request.getReceiverCountry()     // receiverCountry
);
TransactionCategory category = complianceService.categorizeTransaction(catReq);
```

> **Watch out — variable naming:**
> - Don't name `CategoryRequest` as `request` — it shadows the method parameter! Use `catReq`.
> - Whatever you name the `TransactionCategory` result (`category`, `transactionCategory`, etc.), **use that same name consistently** in Steps 4 and 5 when you call `.getCategory()` on it.

The result gives you: `.getCategory()`, `.getSubCategory()`, `.getRegulatoryFlags()`

**Step 3: Fraud screening via Nexus** (async call — new pattern)

Build a `RiskScreeningRequest`. The constructor is:
```
RiskScreeningRequest(transactionId, amount, senderCountry, receiverCountry, description)
```

> **Note:** The parameter order is different from `CategoryRequest`! `CategoryRequest` has `description` third, `RiskScreeningRequest` has it last. Check the constructors if unsure (Ctrl+click in your IDE).

```java
RiskScreeningRequest screenReq = new RiskScreeningRequest(
        request.getTransactionId(),      // transactionId
        request.getAmount(),             // amount
        request.getSenderCountry(),      // senderCountry
        request.getReceiverCountry(),    // receiverCountry
        request.getDescription()         // description (last!)
);
```

Now the async Nexus call — two lines:
```java
NexusOperationHandle<RiskScreeningResult> handle =
        Workflow.startNexusOperation(complianceService::screenTransaction, screenReq);
RiskScreeningResult riskResult = handle.getResult().get();
```

Line 1 starts a `FraudDetectionWorkflow` on the Compliance side (via the Nexus handler you built in Phase 3). Line 2 waits for that workflow to complete and gives you the result.

The `riskResult` gives you: `riskResult.getRiskLevel()`, `riskResult.getRiskScore()`, `riskResult.isRequiresApproval()`, `riskResult.isFlaggedSanctions()`

**Step 4: Human approval** (signal pattern — Exercise 06)

Only needed if the risk level requires approval:

```java
if (riskResult.isRequiresApproval()) {
    Workflow.getLogger(PaymentProcessingWorkflowImpl.class)
            .info("HIGH RISK - Waiting for human approval signal...");

    // Block until signal arrives or 24 hours pass
    boolean signalReceived = Workflow.await(
            Duration.ofHours(24),
            () -> approvalReceived   // ← this field is set by the @SignalMethod below
    );

    if (!signalReceived) {
        return new PaymentResult(false, request.getTransactionId(), "TIMEOUT",
                riskResult.getRiskLevel(), category.getCategory(), null,
                "No approval received within 24 hours");
    }
    if (!approved) {
        return new PaymentResult(false, request.getTransactionId(), "REJECTED",
                riskResult.getRiskLevel(), category.getCategory(), null,
                "Rejected by " + reviewerName + ": " + approvalReason);
    }
}
```

> **How it works:** `Workflow.await()` pauses the workflow (durably — survives crashes!). When someone sends a signal via CLI or the Approvals page, your `@SignalMethod` sets `approvalReceived = true`, which unblocks the `await()`. This is the pattern from Exercise 06.

**Step 5: Execute payment** (activity call — you know this)

```java
String confirmationNumber = paymentActivity.executePayment(request);
return new PaymentResult(true, request.getTransactionId(), "COMPLETED",
        riskResult.getRiskLevel(), category.getCategory(), confirmationNumber, null);
```

#### Don't forget the `@SignalMethod`

The `approveTransaction` method receives the approval signal and sets the fields that `Workflow.await()` checks:

```java
@Override
public void approveTransaction(ApprovalDecision decision) {
    this.approved = decision.isApproved();
    this.reviewerName = decision.getReviewerName();
    this.approvalReason = decision.getReason();
    this.approvalReceived = true;  // ← this unblocks the await()
}
```

#### Data flow cheat sheet

Here's how data flows through the whole workflow:

```
PaymentRequest (input)
    │
    ├─► Step 1: paymentActivity.validatePayment(request) → boolean
    │
    ├─► Step 2: new CategoryRequest(txnId, amount, description, sender, receiver)
    │            complianceService.categorizeTransaction(catReq) → TransactionCategory category
    │            └── category.getCategory()  ← reused in Steps 4 and 5!
    │
    ├─► Step 3: new RiskScreeningRequest(txnId, amount, sender, receiver, description)
    │            Workflow.startNexusOperation(...) → handle → RiskScreeningResult
    │            └── riskResult.getRiskLevel(), .getRiskScore(), .isRequiresApproval()
    │
    ├─► Step 4: if (riskResult.isRequiresApproval())
    │                Workflow.await(24h, () -> approvalReceived)
    │                ← approveTransaction() signal sets the fields
    │
    └─► Step 5: paymentActivity.executePayment(request) → String confirmationNumber
                 return new PaymentResult(success, txnId, status, riskLevel, category, conf, error)
```

> **Checkpoint:** The workflow is complete. Now you need the worker and the starter.

---

### Phase 5: Worker + Starter (wiring it up)

#### The CRAWL Pattern — Every Worker Follows These 5 Steps

> **"Workers CRAWL before they run."**

| Step | Letter | What you do | Code |
|------|--------|-------------|------|
| 1 | **C** — Connect | Connect to Temporal server | `WorkflowServiceStubs` + `WorkflowClient` |
| 2 | **R** — Register | Register workflow types on the worker | `worker.registerWorkflowImplementationTypes(...)` |
| 3 | **A** — Activities | Register activity implementations (inject dependencies) | `worker.registerActivitiesImplementations(...)` |
| 4 | **W** — Wire | Wire up special config (Nexus endpoints, interceptors) | `setNexusServiceOptions(...)` or skip if none |
| 5 | **L** — Launch | Launch the worker — start polling the task queue | `factory.start()` |

For simple workers (no Nexus), skip **W** and it's just C-R-A-L. In this exercise, both workers use all 5 steps.

**File 6: `payments/temporal/PaymentsWorkerApp.java`**

Standard worker pattern from previous exercises. The full `main()` method:

```java
public static void main(String[] args) {
    // C — Connect to Temporal
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    // R — Register workflow types (+ W — Wire Nexus endpoint mapping)
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);   // "payments-processing"
    worker.registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                    .setNexusServiceOptions(Collections.singletonMap(
                            "ComplianceNexusService",
                            NexusServiceOptions.newBuilder()
                                    .setEndpoint("compliance-endpoint")
                                    .build()))
                    .build(),
            PaymentProcessingWorkflowImpl.class);

    // A — Activities (inject business logic dependencies)
    PaymentGateway gateway = new PaymentGateway();
    worker.registerActivitiesImplementations(new PaymentActivityImpl(gateway));

    // L — Launch!
    factory.start();
}
```

The only new thing is step 3 — instead of the simple `worker.registerWorkflowImplementationTypes(MyWorkflow.class)` from previous exercises, we add a `NexusServiceOptions` map. This tells Temporal: "When this workflow uses a `ComplianceNexusService` stub, route those calls to the `compliance-endpoint` that we registered via CLI."

> **Analogy:** Like configuring `compliance.api.url=http://compliance:8080` in a Spring app. The workflow defines WHAT it calls, the worker config defines WHERE.

> **Compare with `ComplianceWorkerApp.java`** (already provided): That worker registers `registerNexusServiceImplementation(new ComplianceNexusServiceImpl())` to HANDLE incoming Nexus requests. This worker registers `setNexusServiceOptions(...)` to SEND outgoing Nexus requests. Handler side vs caller side.

**File 7: `payments/temporal/PaymentStarter.java`**

This starts all 5 payment workflows in parallel. The core loop:

```java
WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
WorkflowClient client = WorkflowClient.newInstance(service);

List<CompletableFuture<PaymentResult>> futures = new ArrayList<>();

for (PaymentRequest txn : transactions) {
    // Business ID workflow ID: "payment-TXN-001" (not a random UUID!)
    String workflowId = "payment-" + txn.getTransactionId();

    PaymentProcessingWorkflow workflow = client.newWorkflowStub(
            PaymentProcessingWorkflow.class,
            WorkflowOptions.newBuilder()
                    .setTaskQueue(TASK_QUEUE)       // "payments-processing"
                    .setWorkflowId(workflowId)      // "payment-TXN-001"
                    .build());

    // execute() returns a CompletableFuture — starts the workflow without blocking
    futures.add(WorkflowClient.execute(workflow::processPayment, txn));
}
```

Then wait for results:
```java
for (int i = 0; i < futures.size(); i++) {
    PaymentResult result = futures.get(i).get();  // blocks until this workflow completes
    System.out.println(result.getTransactionId() + " → " + result.getStatus());
}
```

> **Heads up:** Low-risk transactions (TXN-001, TXN-003) will complete quickly. But high-risk ones (TXN-002, TXN-004, TXN-005) will BLOCK here waiting for approval signals. That's expected! Send signals from another terminal (see "Terminal 7" below) and watch them unblock one by one.

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
