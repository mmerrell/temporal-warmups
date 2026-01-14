# Exercise 06a - Parallel Ticket Processing (Java)

This exercise demonstrates two key patterns for production Temporal workflows:
1. **Parallel Workflow Execution** - Process 10 tickets simultaneously
2. **Business Identifier Workflow IDs** - Use meaningful IDs like `triage-TKT-001`

---

## New Patterns Introduced

### Pattern 1: Parallel Workflow Execution

Start multiple workflows concurrently instead of waiting for each to complete.

**Before (Sequential - SLOW):**
```java
for (String ticket : tickets) {
    // BLOCKS until workflow completes
    TriageResult result = workflow.triageTicket(ticketId, ticketText);
    // Must wait ~2 seconds per ticket
}
// 10 tickets = ~20 seconds total
```

**After (Parallel - FAST):**
```java
List<CompletableFuture<TriageResult>> futures = new ArrayList<>();

for (String ticket : tickets) {
    // Returns IMMEDIATELY - workflow runs in background
    CompletableFuture<TriageResult> future =
        WorkflowClient.execute(workflow::triageTicket, ticketId, ticketText);
    futures.add(future);
}

// Wait for ALL to complete at once
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
// 10 tickets = ~2-3 seconds total
```

**When to use:**
- Batch processing (import 1000 orders)
- Independent workflows (no dependencies between them)
- High-throughput requirements

---

### Pattern 2: Business Identifier Workflow IDs

Use meaningful workflow IDs based on business entities instead of random UUIDs.

**Before (UUID - Hard to find):**
```java
String workflowId = TASK_QUEUE + "-" + UUID.randomUUID();
// Result: "support-triage-a3f7b2c1-8d4e-4f5a-9b3c-1e2d3f4a5b6c"
// Problem: Can't search by ticket ID!
```

**After (Business ID - Easy to find):**
```java
String workflowId = "triage-" + ticketId;
// Result: "triage-TKT-001"
// Benefit: Search by ticket ID in Temporal UI!
```

**Format recommendation:** `{workflow-type}-{business-id}`
- `triage-TKT-001` (ticket)
- `order-ORD-12345` (order)
- `payment-PAY-98765` (payment)

**When to use:**
- Any workflow tied to a business entity
- Need to find workflows by business ID
- Idempotency (only one workflow per entity)

---

## How to Run

### Prerequisites

1. **Temporal Server:**
   ```bash
   temporal server start-dev
   ```

2. **OpenAI API Key:** (for LLM-powered triage)
   ```bash
   export OPENAI_API_KEY=sk-...
   ```

### Terminal 1: Start Worker
```bash
cd exercise-06a-parallel-tickets
mvn compile exec:java@worker
```

### Terminal 2: Run Parallel Starter
```bash
mvn compile exec:java@workflow
```

---

## What to Observe

### 1. Parallel Start
All 10 workflows start within milliseconds:
```
Starting 10 workflows in PARALLEL...

  Started: triage-TKT-001
  Started: triage-TKT-002
  ...
  Started: triage-TKT-010

All 10 workflows started in 47ms
```

### 2. Temporal UI (http://localhost:8233)
- All 10 workflows visible immediately
- Business IDs: `triage-TKT-001` through `triage-TKT-010`
- Click any workflow to see execution details

### 3. Performance
- **Parallel:** 10 tickets in ~3-5 seconds
- **Sequential:** Would take ~20+ seconds
- Worker processes multiple workflows simultaneously

---

## Performance Comparison

| Aspect | Sequential | Parallel |
|--------|------------|----------|
| 10 workflows | ~20 seconds | ~3 seconds |
| Worker utilization | 10% | 100% |
| Client blocking | Per workflow | Once (at end) |
| Throughput | 0.5/sec | 3+/sec |

---

## Files Modified from Exercise-06

| File | Change |
|------|--------|
| `Starter.java` | Parallel execution with `WorkflowClient.execute()` |
| `SupportTriageWorkflowImpl.java` | Added `AUTO_APPROVE_FOR_DEMO` flag |
| `pom.xml` | Updated artifact name |

---

## Real-World Applications

- **Batch imports:** Process 1000 orders from CSV in parallel
- **Event processing:** Handle flood of webhooks simultaneously
- **Report generation:** Generate 50 reports at once
- **Data migration:** Migrate users in parallel batches
