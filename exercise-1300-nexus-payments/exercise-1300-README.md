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

## Files to Complete

| File | Team | Purpose |
|------|------|---------|
| `shared/nexus/ComplianceNexusService.java` | Shared | Nexus service interface (contract) |
| `compliance/temporal/activity/FraudDetectionActivity.java` | Compliance | Activity interface |
| `compliance/temporal/activity/FraudDetectionActivityImpl.java` | Compliance | Activity implementation |
| `compliance/temporal/FraudDetectionWorkflow.java` | Compliance | Workflow interface |
| `compliance/temporal/FraudDetectionWorkflowImpl.java` | Compliance | Workflow implementation |
| `compliance/temporal/ComplianceNexusServiceImpl.java` | Compliance | **Nexus handler (KEY)** |
| `compliance/temporal/ComplianceWorkerApp.java` | Compliance | Worker with Nexus registration |
| `payments/temporal/activity/PaymentActivity.java` | Payments | Activity interface |
| `payments/temporal/activity/PaymentActivityImpl.java` | Payments | Activity implementation |
| `payments/temporal/PaymentProcessingWorkflow.java` | Payments | Workflow interface + @SignalMethod |
| `payments/temporal/PaymentProcessingWorkflowImpl.java` | Payments | **Main workflow (Nexus + Signals)** |
| `payments/temporal/PaymentsWorkerApp.java` | Payments | Worker |
| `payments/temporal/PaymentStarter.java` | Payments | Starts 5 payments in parallel |

## Step 3: Implement Temporal Nexus

Now convert the pre-Temporal baseline into durable Temporal Nexus workflows. Keep the dashboard running — when you start the Temporal workers and run the starter, the UI will automatically switch to the green **"Temporal Nexus Mode"** with richer workflow data.

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
