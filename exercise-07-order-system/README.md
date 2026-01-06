# Exercise 06 - Order Fulfillment with Parent-Child Workflows (Original Version)

## The Problem This Exercise Solves

When processing an order, you have multiple independent steps:
1. Process payment
2. Reserve inventory
3. Arrange shipping
4. Send customer notification

In a monolithic approach (current code), everything happens in one giant function. What are the problems?

## Running the Demo

```bash
python order_system.py
```

Run it a few times - you'll see random failures at payment (~15%), inventory (~20%), or shipping (~10%).

## The Problems with Monolithic Approach

### 1. **No Independent Monitoring**
```
How long did payment take? → Can't tell
Is inventory reservation stuck? → Can't monitor separately
Did shipping fail or succeed? → Lost in one big workflow history
```

### 2. **Can't Retry Individual Steps**
```
Payment succeeded, inventory failed → Can't retry just inventory
Have to retry the ENTIRE order from scratch
```

### 3. **Poor Visibility**
```
Where exactly did it fail? → Have to dig through logs
What's the state of each step? → No separate tracking
Can I see payment history separately? → No
```

### 4. **No Compensations**
When the order fails partway through:
```
✅ Payment processed ($1,059.97 charged)
✅ Inventory reserved (2 items locked)
❌ Shipping failed
Result: Customer charged, inventory stuck, no shipment!
```

The code just logs warnings but doesn't actually clean up!

## What Parent-Child Workflows Solve

### **Parent Workflow:** OrderFulfillmentWorkflow
- Orchestrates the overall order
- Handles failures and compensations
- Maintains order-level state

### **Child Workflows:**
- **PaymentWorkflow** - Independent payment processing with retries
- **InventoryWorkflow** - Manages reservations, can be queried independently
- **ShippingWorkflow** - Arranges shipment, tracks progress
- **NotificationWorkflow** - Sends emails (can fail without affecting order)

### Benefits:

✅ **Independent Monitoring:**
```
Temporal UI shows:
  OrderFulfillmentWorkflow (parent)
    ├── PaymentWorkflow (completed in 2.3s)
    ├── InventoryWorkflow (completed in 1.1s)
    ├── ShippingWorkflow (running...)
    └── NotificationWorkflow (pending)
```

✅ **Retry Individual Steps:**
```
Inventory failed? → Just retry InventoryWorkflow
Payment succeeded → Don't retry payment
```

✅ **Better Debugging:**
```
Each child workflow has its own history
Can query payment status independently
Can see exactly where it failed
```

✅ **Proper Compensations:**
```
❌ Shipping fails
   ↓ Parent compensates:
✅ Release inventory reservation
✅ Refund payment
Result: Clean state, customer not charged!
```

## Your Challenge

Convert this monolithic code to use Temporal parent-child workflows:

1. Create child workflows for: Payment, Inventory, Shipping, Notification
2. Create parent workflow that orchestrates the children
3. Add compensations when children fail
4. Ensure each child can be monitored independently
5. Make sure retries work at the child level

## Expected Flow

**With parent-child workflows:**

```python
@workflow.defn
class OrderFulfillmentWorkflow:
    @workflow.run
    async def run(self, order: Order):
        # Start child workflows
        payment = await workflow.start_child_workflow(
            PaymentWorkflow.run,
            args=[order],
            id=f"payment-{order.order_id}"
        )
        
        inventory = await workflow.start_child_workflow(
            InventoryWorkflow.run,
            args=[order],
            id=f"inventory-{order.order_id}"
        )
        
        # ... etc
```

Each child workflow can now be:
- Monitored independently in Temporal UI
- Retried individually if it fails
- Queried for status
- Debugged with its own history

## Success Criteria

When you're done:
- ✅ Parent workflow orchestrates 4 child workflows
- ✅ Each child has its own workflow ID (e.g., `payment-ORDER-001`)
- ✅ Children run sequentially (payment → inventory → shipping → notification)
- ✅ Parent compensates when children fail (refund payment, release inventory)
- ✅ Temporal UI shows parent + children separately
- ✅ Can query individual child workflow status

## Estimated Time

**2-3 hours** for a complete implementation with compensations.

## Next Steps

After completing this exercise (Exercise 6), you'll move to **Exercise 7: Parallel Execution**, where you'll modify this same scenario to run multiple children in parallel instead of sequentially.