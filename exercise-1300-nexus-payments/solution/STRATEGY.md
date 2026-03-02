# Strategy Guide: Exercise 1300 - Cross-Team Payment Compliance via Nexus

## The Problem

The Payments and Compliance teams communicate via REST. This causes:

1. **Cascading failures** - Compliance outage blocks ALL payments
2. **Lost transactions** - Crash mid-process, no recovery
3. **No audit trail** - Can't trace cross-team decisions
4. **Brittle contracts** - REST API changes break both teams
5. **No human approval** - High-risk transactions auto-processed
6. **No retries** - Transient failures are permanent

## What is Nexus?

Think of Nexus as a **durable RPC framework** between Temporal applications:

```
┌──────────────────┐         ┌──────────────────────┐
│  Payments Team   │  Nexus  │  Compliance Team     │
│                  │ ──────► │                      │
│  PaymentWorkflow │         │  FraudDetection WF   │
│  (caller)        │ ◄────── │  Categorizer Agent   │
│                  │         │  (handler)           │
└──────────────────┘         └──────────────────────┘
```

**Nexus vs REST:**
| | REST | Nexus |
|---|------|-------|
| Durability | None | Full (survives crashes) |
| Retries | Manual | Automatic |
| Type safety | OpenAPI/manual | Compile-time |
| Visibility | Logs only | Temporal UI shows both sides |
| Timeouts | HTTP timeouts | Temporal-managed |
| Audit trail | Build yourself | Built-in event history |

## Step-by-Step Implementation

### Step 1: Run the Baseline

```bash
mvn compile exec:java
```

Observe the problems. Note which transactions fail and why.

### Step 2: Create the Nexus Endpoint

Before any code runs, you need to tell Temporal about the cross-team connection:

```bash
temporal operator nexus endpoint create \
  --name compliance-endpoint \
  --target-namespace default \
  --target-task-queue compliance-risk
```

This says: "When someone calls 'compliance-endpoint', route to workers on 'compliance-risk' task queue."

### Step 3: Understand the Architecture

```
PaymentStarter
  └── starts PaymentProcessingWorkflow (payments-processing queue)
         ├── validatePayment() ......... local activity
         ├── categorizeTransaction() ... Nexus SYNC call → ComplianceNexusService
         ├── screenTransaction() ....... Nexus ASYNC call → starts FraudDetectionWorkflow
         │                                                  on compliance-risk queue
         ├── [if high risk] ............ Workflow.await() for approval Signal
         └── executePayment() .......... local activity
```

### Step 4: Compliance Side (Handler)

Build from the bottom up:

1. **FraudDetectionActivity** - Interface + Impl wrapping FraudDetectionAgent
2. **FraudDetectionWorkflow** - Interface + Impl orchestrating the activity
3. **ComplianceNexusServiceImpl** - The Nexus handler (KEY NEW FILE)
   - `screenTransaction()` → starts FraudDetectionWorkflow (ASYNC)
   - `categorizeTransaction()` → runs inline (SYNC)
4. **ComplianceWorkerApp** - Registers workflow + activities + **Nexus handler**

Key new line in worker:
```java
worker.registerNexusServiceImplementation(new ComplianceNexusServiceImpl());
```

### Step 5: Shared Contract

**ComplianceNexusService** interface - both teams depend on this:
- `screenTransaction(RiskScreeningRequest)` → `RiskScreeningResult`
- `categorizeTransaction(CategoryRequest)` → `TransactionCategory`

This is like a shared proto/API definition. Changes require coordination.

### Step 6: Payments Side (Caller)

1. **PaymentActivity** - Interface + Impl for validate/execute via PaymentGateway
2. **PaymentProcessingWorkflow** - Interface with `@SignalMethod approveTransaction()`
3. **PaymentProcessingWorkflowImpl** - The main workflow:
   - Creates Nexus service stub: `Workflow.newNexusServiceStub(ComplianceNexusService.class, ...)`
   - Calls `complianceService.categorizeTransaction(...)` (sync Nexus)
   - Calls `complianceService.screenTransaction(...)` (async Nexus)
   - Uses `Workflow.await()` for high-risk approval (same as Ex 06)
4. **PaymentsWorkerApp** - Standard worker
5. **PaymentStarter** - Starts 5 payments in parallel (Ex 06a pattern)

Key new lines in workflow:
```java
ComplianceNexusService compliance = Workflow.newNexusServiceStub(
    ComplianceNexusService.class,
    NexusServiceOptions.newBuilder()
        .setEndpoint("compliance-endpoint")  // matches CLI endpoint name
        .setOperationOptions(...)
        .build());
```

### Step 7: Run Everything

Terminal 1: `temporal server start-dev`
Terminal 2: `temporal operator nexus endpoint create --name compliance-endpoint --target-namespace default --target-task-queue compliance-risk`
Terminal 3: `mvn compile exec:java@compliance-worker`
Terminal 4: `mvn compile exec:java@payments-worker`
Terminal 5: `mvn compile exec:java@starter`

### Step 8: Send Approval Signals

For high-risk transactions waiting for approval:

```bash
# Approve TXN-002 (Cayman Islands transfer)
temporal workflow signal --workflow-id payment-TXN-002 \
  --name approveTransaction \
  --input '{"approved":true,"reviewerName":"Jane","reason":"Verified client identity"}'

# Reject TXN-004 (Russia sanctions)
temporal workflow signal --workflow-id payment-TXN-004 \
  --name approveTransaction \
  --input '{"approved":false,"reviewerName":"Jane","reason":"Sanctions violation"}'

# Approve TXN-005 (Structuring concern)
temporal workflow signal --workflow-id payment-TXN-005 \
  --name approveTransaction \
  --input '{"approved":true,"reviewerName":"Bob","reason":"Recurring deposit verified"}'
```

### Step 9: Observe in Temporal UI

1. Open http://localhost:8233
2. Find `payment-TXN-001` - see it completed with Nexus calls
3. Find `fraud-screen-TXN-002` - see the Compliance workflow started by Nexus
4. See the **linked** workflows - Nexus connects them visually

### Step 10: Test Durability

1. Kill the Compliance worker (Ctrl+C Terminal 3)
2. Start a new payment: `mvn compile exec:java@starter`
3. Payment workflow starts, Nexus call to Compliance hangs (no handler)
4. Restart Compliance worker
5. Fraud detection resumes automatically - Nexus retries!

## Key Concepts Summary

| Concept | Where | Why |
|---------|-------|-----|
| Nexus Service Interface | `shared/nexus/` | Type-safe cross-team contract |
| Nexus Handler (async) | `ComplianceNexusServiceImpl.screenTransaction()` | Starts a workflow via Nexus |
| Nexus Handler (sync) | `ComplianceNexusServiceImpl.categorizeTransaction()` | Quick inline operation |
| Nexus Caller | `PaymentProcessingWorkflowImpl` | `Workflow.newNexusServiceStub()` |
| Nexus Endpoint | CLI command | Routes requests to correct task queue |
| Signal (review) | `approveTransaction()` | Human-in-the-loop (from Ex 06) |
| Parallel Start | `PaymentStarter` | `WorkflowClient.execute()` (from Ex 06a) |
| Business IDs | `"payment-TXN-001"` | Meaningful workflow IDs (from Ex 06a) |
