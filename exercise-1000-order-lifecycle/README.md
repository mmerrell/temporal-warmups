# Exercise 1000 - Order Lifecycle Management

## Overview

This exercise teaches **full order lifecycle management** from cart to doorstep using Temporal. You'll convert a monolithic order processing service into a durable workflow with proper activity separation, compensation, and — the **new concept** — **Queries** for reading workflow state mid-execution.

**Difficulty:** 4/5 (Intermediate-Advanced)
**Language:** Java
**New Concept:** `@QueryMethod` — read workflow state without side effects
**Prerequisites:** Exercises 01-05

## Scenario

You're building the backend for an e-commerce platform. An order goes through 6 stages: validation → payment → inventory reservation → shipment creation → delivery tracking → notification. The current code is a monolithic service that blocks threads, loses state on crash, and orphans payments when downstream steps fail.

Your job: rebuild it with Temporal so that every order is **durable**, **observable**, and **self-healing**.

## Quickstart Docs By Temporal

🚀 [Get started in a few mins](https://docs.temporal.io/quickstarts?utm_campaign=awareness-nikolay-advolodkin&utm_medium=code&utm_source=github)

## Original Code

Run the pre-Temporal version to see the problems firsthand:

```bash
cd exercise-1000-order-lifecycle/
mvn compile exec:java
```

Open `src/main/java/exercise/OrderLifecycleService.java` and look for these **problems**:

| Problem | Where | Why It's Bad |
|---------|-------|-------------|
| `Thread.sleep(2000)` in delivery loop | Step 5 | Blocks a thread for seconds (days in production!) |
| `Math.random()` / `System.currentTimeMillis()` | Steps 2-5 | Non-deterministic — breaks Temporal replay |
| `System.out.println()` everywhere | All steps | Not replay-safe — duplicates on replay |
| No status query from outside JVM | `getOrderStatus()` | Only works in-process — other services can't see it |
| No compensation | Steps 3-4 | Payment orphaned if inventory/shipping fails |
| Manual retry loop | Step 2 | Hardcoded 3 attempts — should use retry policies |
| In-memory `HashMap` state | Top of class | Everything lost on process crash |

## Learning Goals

After this exercise, you will be able to:

1. **Implement `@QueryMethod`** to expose order status and tracking info to any external client
2. **Use `WorkflowClient.start()`** to launch workflows non-blocking and query them while they run
3. **Use `@SignalMethod`** for human-in-the-loop approval after payment
4. **Convert blocking delivery polling** to a `Workflow.sleep()` + activity loop
5. **Manage order status transitions** using workflow instance fields
6. **Implement 8 activities** with appropriate retry policies
7. **Add compensation** (refund payment + release inventory) when downstream steps fail

## Key Concepts

### Queries — Read Workflow State Without Side Effects

Queries let **any external client** read the current state of a running workflow — no shared database needed. The workflow's instance fields ARE the state.

```java
// In the workflow interface:
@WorkflowInterface
public interface OrderLifecycleWorkflow {

    @WorkflowMethod
    OrderResult processOrder(Order order);

    @QueryMethod
    String getOrderStatus();       // Any client can call this!

    @QueryMethod
    OrderTrackingInfo getTrackingInfo();  // Returns tracking details
}

// In the workflow implementation:
public class OrderLifecycleWorkflowImpl implements OrderLifecycleWorkflow {

    private String currentStatus = "CREATED";
    private String trackingNumber = null;

    @Override
    public String getOrderStatus() {
        return currentStatus;    // Just return the field — no side effects!
    }

    @Override
    public OrderTrackingInfo getTrackingInfo() {
        return new OrderTrackingInfo(currentStatus, trackingNumber);
    }

    @Override
    public OrderResult processOrder(Order order) {
        currentStatus = "VALIDATED";
        // ... activities change currentStatus as order progresses
    }
}
```

**From any client:**
```java
WorkflowStub handle = client.newUntypedWorkflowStub("order-ORD-001");
String status = handle.query("getOrderStatus", String.class);
System.out.println("Current status: " + status);  // e.g., "IN_TRANSIT"
```

**Rules for `@QueryMethod`:**
- Must be **read-only** — no side effects, no activity calls, no `Workflow.sleep()`
- Just return instance field values
- Can be called at any time, even while workflow is sleeping during delivery tracking

### Signals — Human-in-the-Loop Approval

After payment is processed, the workflow pauses and waits for a human to approve the order before proceeding to inventory and shipping. This uses `@SignalMethod` — an external message sent INTO a running workflow.

**In the workflow interface:**
```java
@SignalMethod
void approveOrder();
```

**In the workflow implementation:**
```java
private boolean approvalReceived = false;

@Override
public void approveOrder() {
    this.approvalReceived = true;  // Signal handler — just flip the flag
}
```

**In `processOrder()`, after payment:**
```java
this.paymentId = activities.processPayment(...);
currentStatus = "AWAITING_APPROVAL";

// Workflow suspends here — durable, no thread held
boolean approved = Workflow.await(Duration.ofMinutes(5), () -> approvalReceived);
if (!approved) {
    // Timeout — refund and fail
    activities.refundPayment(paymentId);
    currentStatus = "APPROVAL_TIMEOUT";
    return "FAILED";
}
currentStatus = "APPROVED";
// Continue to inventory reservation...
```

**Key points about Signals:**
- `@SignalMethod` must return `void` — it's fire-and-forget
- `Workflow.await()` is durable — survives worker crashes
- Always use a timeout to avoid infinite waits
- Signals are persisted — even if the workflow hasn't reached the `await` yet, the signal is stored and applied when it does

#### Approving via Temporal CLI

You can send the signal from the command line without writing any Java code:

```bash
# Approve a specific order
temporal workflow signal --workflow-id order-ORD-001 --name approveOrder
```

#### Approving via Temporal UI

1. Open http://localhost:8233
2. Find the workflow (e.g., `order-ORD-001`)
3. Click the workflow to open its detail page
4. Click **Signal** in the top-right
5. Enter signal name: `approveOrder`
6. Click **Submit**

#### Approving via Java (in Starter.java)

```java
// Get a handle to the running workflow and send the signal
OrderLifecycleWorkflow handle = client.newWorkflowStub(
        OrderLifecycleWorkflow.class, "order-" + order.orderId);
handle.approveOrder();
```

#### Querying status while waiting for approval

While the workflow is paused at `AWAITING_APPROVAL`, you can query it:

```bash
temporal workflow query --workflow-id order-ORD-001 --name getOrderStatus
```

This returns `"AWAITING_APPROVAL"` — demonstrating how Queries and Signals work together.

### Long-Running Workflows — Delivery Tracking

The pre-Temporal code uses `Thread.sleep(2000)` in a loop to poll delivery status. In production, delivery takes **days**. You can't hold a thread for days!

With Temporal, `Workflow.sleep()` is **durable** — the workflow suspends, the worker is free, and Temporal wakes it up when the timer fires. Even if the worker crashes and restarts, the sleep resumes exactly where it left off.

**The polling pattern:**
```java
// In the workflow:
String deliveryStatus = "in_transit";
while (!"delivered".equals(deliveryStatus)) {
    deliveryStatus = Workflow.newActivityStub(OrderActivities.class, deliveryOptions)
            .checkDeliveryStatus(trackingNumber);

    if (!"delivered".equals(deliveryStatus)) {
        Workflow.sleep(Duration.ofMinutes(30));  // Durable! Not Thread.sleep!
    }
}
currentStatus = "DELIVERED";
```

### Compensation — Undo What Succeeded

If shipment creation fails after payment was charged and inventory reserved, you must:
1. Release the inventory (`releaseInventory`)
2. Refund the payment (`refundPayment`)

Track what succeeded using instance fields so you know what to compensate:

```java
String paymentId = null;
String reservationId = null;

try {
    paymentId = activities.processPayment(...);
    currentStatus = "PAID";

    reservationId = activities.reserveInventory(...);
    currentStatus = "FULFILLING";

    trackingNumber = activities.createShipment(...);
    currentStatus = "SHIPPED";

} catch (ActivityFailure e) {
    // Compensate in reverse order — only what succeeded
    if (reservationId != null) {
        activities.releaseInventory(reservationId);
    }
    if (paymentId != null) {
        activities.refundPayment(paymentId);
    }
    throw e;  // or return a failure result
}
```

## What You'll Build

### File Structure

```
src/main/java/solution/temporal/
├── OrderLifecycleWorkflow.java       # Workflow interface
├── OrderLifecycleWorkflowImpl.java   # Workflow implementation
├── OrderActivities.java              # Activity interface (8 methods)
├── OrderActivitiesImpl.java          # Activity implementations
├── OrderResult.java                  # Workflow return type (simple POJO)
├── OrderTrackingInfo.java            # Query return type (simple POJO)
├── WorkerApp.java                    # Worker setup
└── Starter.java                      # Client + query demo
```

### 1. Workflow Interface — `OrderLifecycleWorkflow.java`

Define:
- `@WorkflowMethod processOrder(Order order)` → returns `OrderResult`
- `@QueryMethod getOrderStatus()` → returns `String`
- `@QueryMethod getTrackingInfo()` → returns `OrderTrackingInfo`

### 2. Activities Interface — `OrderActivities.java`

8 activity methods:

| Activity | Input | Output | Notes |
|----------|-------|--------|-------|
| `validateOrder(Order)` | Order | void | Throws on invalid |
| `processPayment(orderId, email, amount)` | String, String, double | String (paymentId) | Can fail — use retries |
| `reserveInventory(orderId, items)` | String, List\<OrderItem\> | String (reservationId) | Can fail — use retries |
| `createShipment(orderId, address)` | String, String | String (trackingNumber) | Can fail — use retries |
| `checkDeliveryStatus(trackingNumber)` | String | String | Returns "in_transit", "out_for_delivery", or "delivered" |
| `sendNotification(email, orderId, status)` | String, String, String | void | Non-critical — don't compensate if fails |
| `refundPayment(paymentId)` | String | void | Compensation activity |
| `releaseInventory(reservationId)` | String | void | Compensation activity |

### 3. Workflow Implementation — Deciding What Goes Where

| Step | Workflow or Activity? | Why? |
|------|----------------------|------|
| Update `currentStatus` field | **Workflow** | Pure state management, no I/O |
| Validate order items | **Activity** | Could involve database lookup in production |
| Charge payment | **Activity** | External payment gateway call |
| Reserve inventory | **Activity** | Database write |
| Create shipment | **Activity** | External shipping API call |
| Check delivery status | **Activity** | External tracking API call |
| Sleep between checks | **Workflow** (`Workflow.sleep()`) | Durable timer, not Thread.sleep |
| Send email | **Activity** | External email service call |
| Refund payment | **Activity** | External payment gateway call |
| Release inventory | **Activity** | Database write |

### 4. Retry Policy Guidance

```java
// For most activities — retries with backoff
RetryOptions retryOptions = RetryOptions.newBuilder()
        .setInitialInterval(Duration.ofSeconds(1))
        .setMaximumInterval(Duration.ofSeconds(30))
        .setBackoffCoefficient(2.0)
        .setMaximumAttempts(5)
        .build();

ActivityOptions options = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(5))
        .setRetryPolicy(retryOptions)
        .build();

// For delivery check — shorter timeout, fewer retries
ActivityOptions deliveryCheckOptions = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))
        .setRetryPolicy(RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .build())
        .build();

// For notifications — non-critical, limited retries
ActivityOptions notificationOptions = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))
        .setRetryPolicy(RetryOptions.newBuilder()
                .setMaximumAttempts(2)
                .build())
        .build();
```

### 5. Worker — `WorkerApp.java`

- Task queue: `"order-lifecycle"`
- Register: `OrderLifecycleWorkflowImpl` + `OrderActivitiesImpl`

### 6. Client/Starter — `Starter.java`

#### `WorkflowClient.start()` vs `workflow.processOrder()`

There are two ways to call a workflow:

| Method | Behavior | Returns |
|--------|----------|---------|
| `workflow.processOrder(order)` | **Blocking** — waits until the workflow finishes | The workflow result (`String`) |
| `WorkflowClient.start(workflow::processOrder, order)` | **Non-blocking** — sends the request and returns immediately | `WorkflowExecution` (just the workflow ID + run ID) |

For this exercise, use `WorkflowClient.start()` because:
- Orders take a long time (delivery tracking can span days)
- You want to **start the workflow and then query it** while it's still running
- Blocking would defeat the purpose of demonstrating Queries

#### Why Loop Over Orders?

Each order needs its **own workflow stub** with a **unique workflow ID**. You can't reuse a stub — a stub is tied to one workflow execution. So the loop does 3 things per order:

1. **Create a stub** with a business ID (`"order-" + order.orderId`)
2. **Start the workflow** (non-blocking)
3. Move on to the next order immediately

```java
// Loop 1: Start all workflows (non-blocking)
for (Order order : orders) {
    String workflowId = "order-" + order.orderId;

    // Each order gets its OWN stub — you can't reuse stubs!
    OrderLifecycleWorkflow workflow = client.newWorkflowStub(
            OrderLifecycleWorkflow.class,
            WorkflowOptions.newBuilder()
                    .setTaskQueue("order-lifecycle")
                    .setWorkflowId(workflowId)
                    .build());

    // Fire and forget — workflow runs on the worker, not here
    WorkflowClient.start(workflow::processOrder, order);
    System.out.println("Started workflow: " + workflowId);
}

// Loop 2: Query all workflows after a short delay
Thread.sleep(3000);
for (Order order : orders) {
    String status = client.newUntypedWorkflowStub("order-" + order.orderId)
            .query("getOrderStatus", String.class);
    System.out.println(order.orderId + " status: " + status);
}
```

**Think of it like a restaurant:** `start()` is placing the order with the kitchen (and walking away). `processOrder()` would be standing at the counter until your food is ready. Queries are peeking through the kitchen window to see how your meal is progressing.

## Running Your Solution

### Terminal 1 — Start Worker
```bash
cd exercise-1000-order-lifecycle
mvn compile exec:java@worker
```

### Terminal 2 — Run Client
```bash
cd exercise-1000-order-lifecycle
mvn compile exec:java@client
```

### Temporal UI
Open http://localhost:8233 and look for:
- Workflow with ID `order-ORD-001`
- Click the **Query** tab → call `getOrderStatus` and `getTrackingInfo`
- View **Event History** to see activity executions and status transitions

## Common Issues & Solutions

### Query returns null or stale data
**Cause:** Query method reads a field that hasn't been set yet.
**Fix:** Initialize all query fields in the workflow implementation (e.g., `private String currentStatus = "CREATED";`).

### Delivery polling never ends
**Cause:** `checkDeliveryStatus` activity never returns `"delivered"`.
**Fix:** Add a maximum number of polling iterations (e.g., 20) and treat timeout as a delivery exception.

### Workflow fails with "non-deterministic" error
**Cause:** Using `Math.random()`, `System.currentTimeMillis()`, `new Date()`, or `Thread.sleep()` in workflow code.
**Fix:** Use these **only in activities**. In workflow code, use `Workflow.currentTimeMillis()`, `Workflow.sleep()`, etc.

### "Object is not JSON serializable" error
**Cause:** Using enums, Date objects, or custom types as activity parameters/returns.
**Fix:** Use simple types (String, int, double, List, POJO with default constructor).

### `UnrecognizedPropertyException: Unrecognized field "totalAmount"`
**Cause:** Jackson treats any `public getXxx()` method as a serializable property. So `getTotalAmount()` gets serialized as `"totalAmount"`, but on deserialization there's no matching field — and Jackson throws.
**Fix:** Annotate computed getter methods with `@JsonIgnore`:
```java
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnore
public double getTotalAmount() { ... }
```
**Rule of thumb:** If a `getXxx()` method computes a value rather than returning a field, it needs `@JsonIgnore`. Alternatively, rename it to not start with `get` (e.g., `computeTotalAmount()`).

### Compensation activities fail
**Cause:** Compensation activities should be resilient — use generous retry policies for compensations.
**Fix:** Create separate `ActivityOptions` for compensations with more retries and longer timeouts.

### Replay-safe logging
**Cause:** Using `System.out.println()` in workflow code causes duplicate logs on replay.
**Fix:** Use `Workflow.getLogger(OrderLifecycleWorkflowImpl.class)` in workflow code. Use `Activity.getExecutionContext().getInfo()` or SLF4J in activities.

## Success Criteria

When complete, verify:

- [ ] `mvn compile exec:java` — Pre-temporal code runs and shows problems
- [ ] `mvn compile exec:java@worker` — Worker starts without errors
- [ ] `mvn compile exec:java@client` — Client starts workflow and queries status
- [ ] **Temporal UI** shows workflow with ID `order-ORD-001`
- [ ] **Query tab** in Temporal UI returns current status via `getOrderStatus`
- [ ] **Event History** shows all activity executions in order
- [ ] If shipping fails, **compensation activities** (refund + release inventory) appear in history
- [ ] **No `Thread.sleep()`** in workflow code — only `Workflow.sleep()`
- [ ] **No `Math.random()`** in workflow code — only in activities
- [ ] **No `System.out.println()`** in workflow code — use `Workflow.getLogger()`
- [ ] Delivery polling uses activity + `Workflow.sleep()` loop, not `Thread.sleep()`
- [ ] Kill the worker mid-delivery-tracking, restart it, and the workflow **resumes from where it left off**

## Tips

- **Think of instance fields as your database.** In a Temporal workflow, the workflow's instance fields ARE the durable state. Queries just expose them.
- **Queries are free.** They don't create events in the history and don't affect the workflow's execution. Call them as often as you want.
- **`Workflow.sleep()` is like `Thread.sleep()` but magic.** It doesn't hold a thread. The worker can process other workflows while this one sleeps. If the worker dies and restarts, the sleep picks up right where it left off.
- **Compensation activities should always succeed.** Use generous retry policies for refunds and inventory releases — you don't want compensation to fail and leave the system in an inconsistent state.
- **Start simple.** Get the happy path working first (all 6 steps succeed), then add compensation, then add queries. Don't try to build everything at once.
