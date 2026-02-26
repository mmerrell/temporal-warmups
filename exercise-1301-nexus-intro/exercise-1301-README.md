# Exercise 1301 — Temporal Nexus Intro (Java)

> **Prerequisite to Exercise 1300.** This exercise teaches the core Nexus mental model through a single sync cross-team call. Once you've finished here, Exercise 1300 adds async Nexus, human approval signals, parallel execution, and a full dashboard.

---

## Scenario

The **Payments team** processes financial transactions. Before executing any payment, they must get a compliance check from the **Compliance team** — a separate team with its own codebase, workers, and infrastructure.

Today they do this with a direct REST call. If the Compliance service is down, all payments fail. No retries. No audit trail. No recovery.

## Quickstart Docs By Temporal

🚀 [Get started in a few mins](https://docs.temporal.io/quickstarts?utm_campaign=awareness-nikolay-advolodkin&utm_medium=code&utm_source=github)

**Your mission:** Connect these two teams using **Temporal Nexus** — durable, type-safe, retryable cross-team RPC.

---

## The Architecture

```
PAYMENTS TEAM                            COMPLIANCE TEAM
─────────────────────────────            ──────────────────────────────
PaymentStarter
  └─► PaymentProcessingWorkflow
       │
       ├─ Step 1: validatePayment ──────► PaymentGateway
       │          (activity)
       │
       ├─ Step 2: ──── Nexus SYNC ──────► ComplianceNexusServiceImpl
       │           checkCompliance()       └─► ComplianceAgent (OpenAI)
       │           (new concept!)
       │
       └─ Step 3: executePayment ────────► PaymentGateway
                  (only if approved)
```

### The 3 Test Transactions

| TXN | Amount | Route | Expected Risk | Expected Outcome |
|-----|--------|-------|---------------|-----------------|
| TXN-A | $250 | US → US | LOW | ✅ Approved, processed |
| TXN-B | $12,000 | US → UK | MEDIUM | ✅ Approved with note |
| TXN-C | $75,000 | US → North Korea | HIGH | 🚫 Declined by compliance |

---

## What Is Temporal Nexus?

Think of a Nexus operation as a **durable REST call**:

| Fragile REST | Temporal Nexus |
|---|---|
| HTTP call — fails silently | Durable RPC — retried automatically |
| Tight coupling to URL/IP | Decoupled via named endpoint |
| No retry on failure | Configurable retry policy |
| No audit trail | Full event history in Temporal UI |
| Caller must handle errors | Temporal handles transient failures |

The workflow calls `complianceService.checkCompliance(request)` — it looks like a local Java method call, but Temporal routes it across team boundaries via the Nexus endpoint you configured with the CLI.

---

## What You'll Implement (6 files)

### 1. `PaymentActivityImpl.java` — easiest, ~15 lines
**Pattern:** thin wrapper that delegates to `PaymentGateway`

```java
// Accept PaymentGateway via constructor, then:
public boolean validatePayment(PaymentRequest request) {
    return gateway.validatePayment(request);
}
public String executePayment(PaymentRequest request) {
    return gateway.executePayment(request);
}
```

---

### 2. `ComplianceNexusServiceImpl.java` — the key new concept, ~25 lines
**Pattern:** sync Nexus handler that calls the LLM agent

```java
@ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {
    // Inject ComplianceAgent via constructor

    @OperationImpl
    public OperationHandler<ComplianceRequest, ComplianceResult> checkCompliance() {
        return WorkflowClientOperationHandlers.sync(
            (context, details, client, input) -> agent.checkCompliance(input)
        );
    }
}
```

> **Metaphor:** `@Service` = API route definition. `@ServiceImpl` = the controller. `WorkflowClientOperationHandlers.sync()` = "handle this request inline and return a result immediately."

---

### 3. `ComplianceWorkerApp.java` — one new line vs previous workers, ~20 lines
**Pattern:** standard worker + the new `registerNexusServiceImplementation()`

```java
// Same CRAWL pattern as before, with one addition:
ComplianceAgent agent = new ComplianceAgent();
worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl(agent));
```

> **What's new:** `registerNexusServiceImplementation()` is what tells Temporal "this worker handles incoming Nexus requests."

---

### 4. `PaymentProcessingWorkflowImpl.java` — the main orchestrator, ~45 lines
**Pattern:** create a Nexus stub, call it like a local method

```java
// Create the Nexus stub (looks like a REST client, but durable):
ComplianceNexusService complianceService = Workflow.newNexusServiceStub(
    ComplianceNexusService.class,
    NexusServiceOptions.newBuilder()
        .setOperationOptions(NexusOperationOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofMinutes(2))
            .build())
        .build());

// Then call it like a local method — Temporal handles the cross-team routing:
ComplianceResult compliance = complianceService.checkCompliance(compReq);
if (!compliance.isApproved()) {
    return new PaymentResult(false, txnId, "DECLINED_COMPLIANCE", ...);
}
```

> **In Exercise 1300:** you'll also use `Workflow.startNexusOperation()` for async calls and `Workflow.await()` for human approval signals.

---

### 5. `PaymentsWorkerApp.java` — Nexus endpoint mapping, ~20 lines
**Pattern:** register workflow WITH `NexusServiceOptions`

```java
// The workflow knows WHAT to call (ComplianceNexusService)
// The worker knows WHERE to find it (compliance-endpoint)
worker.registerWorkflowImplementationTypes(
    WorkflowImplementationOptions.newBuilder()
        .setNexusServiceOptions(Collections.singletonMap(
            "ComplianceNexusService",
            NexusServiceOptions.newBuilder()
                .setEndpoint("compliance-endpoint")
                .build()))
        .build(),
    PaymentProcessingWorkflowImpl.class);
```

---

### 6. `PaymentStarter.java` — business ID workflow IDs, ~30 lines
**Pattern:** meaningful workflow IDs + sequential execution

```java
// Business ID pattern (from Exercise 06a):
String workflowId = "payment-" + txn.getTransactionId(); // e.g. "payment-TXN-A"

PaymentProcessingWorkflow wf = client.newWorkflowStub(
    PaymentProcessingWorkflow.class,
    WorkflowOptions.newBuilder()
        .setTaskQueue("payments-processing")
        .setWorkflowId(workflowId)
        .build());

PaymentResult result = wf.processPayment(txn); // blocks until complete
```

> **In Exercise 1300:** you'll start 5 workflows in parallel using `WorkflowClient.execute()` + `CompletableFuture`.

---

## How to Run

### Prerequisites

```bash
# 1. Start Temporal server
temporal server start-dev

# 2. Set your OpenAI API key
export OPENAI_API_KEY="sk-..."

# 3. Create the Nexus endpoint (one-time setup)
temporal operator nexus endpoint create \
  --name compliance-endpoint \
  --target-namespace default \
  --target-task-queue compliance-risk
```

### Running

```bash
# Terminal 1 (already running): temporal server start-dev

# Terminal 2: Start compliance worker
cd exercise-1301-nexus-intro
mvn compile exec:java@compliance-worker

# Terminal 3: Start payments worker
mvn compile exec:java@payments-worker

# Terminal 4: Run all 3 transactions
mvn compile exec:java@starter

# View results in Temporal UI:
open http://localhost:8233

# View interactive diagram:
open ui/index.html
```

### Optional: Run the pre-temporal baseline first

```bash
mvn compile exec:java
```

See the problems (no retries, no durability, tight coupling) before implementing the Temporal solution.

---

## What to Look For in the Temporal UI

After running the starter:

1. **3 payment workflows** in the namespace: `payment-TXN-A`, `payment-TXN-B`, `payment-TXN-C`
2. Click any workflow → **Event History** shows each step with inputs, outputs, and timing
3. Click the **Nexus** tab (or see the linked workflow) to see the cross-team compliance call
4. `payment-TXN-C` should show `DECLINED_COMPLIANCE` status — the `executePayment` step was never called

---

## Interactive Diagram

Open `ui/index.html` directly in your browser (no build step):

```bash
open ui/index.html
```

- Select TXN-A, TXN-B, or TXN-C
- Step through the flow with Next/Back buttons
- Toggle **REST vs Nexus** to see why the old approach fails
- Click any component to learn what it does

---

## Key Concepts Recap

| Concept | What it means |
|---------|---------------|
| `@Service` / `@Operation` | Define the cross-team API contract (shared interface) |
| `@ServiceImpl` / `@OperationImpl` | Implement the handler on the Compliance side |
| `WorkflowClientOperationHandlers.sync()` | Handle Nexus call inline, return result immediately |
| `Workflow.newNexusServiceStub()` | Create a durable "client" for the cross-team service |
| `registerNexusServiceImplementation()` | Tell the worker to handle Nexus requests |
| `NexusServiceOptions.setEndpoint()` | Tell the worker which Nexus endpoint to use |

---

## What's Saved for Exercise 1300

| Feature | 1301 | 1300 |
|---------|:----:|:----:|
| Sync Nexus operation | ✅ | ✅ |
| Async Nexus (starts a workflow) | — | ✅ |
| Human approval via Signals | — | ✅ |
| Two LLM agents | — | ✅ |
| Parallel execution (5 transactions) | — | ✅ |
| Full Next.js dashboard | — | ✅ |

---

## Troubleshooting

**`OPENAI_API_KEY not set`** → `export OPENAI_API_KEY="sk-..."`

**`Nexus endpoint not found`** → Run the `temporal operator nexus endpoint create` command above

**`Task queue mismatch`** → Compliance worker must use `"compliance-risk"` (matches the endpoint target)

**`UnsupportedOperationException: TODO`** → You haven't implemented that file yet — check the TODO comments

**Workflows staying `Running` forever** → One of your workers isn't started, or threw an exception on startup
