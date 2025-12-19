# Exercise 02 - Order Processing System

## Scenario
An e-commerce order processing flow that handles payment, inventory reservation, and shipping. This exercise introduces more complexity than Exercise #1 with multiple activities, external state management, and realistic failure scenarios.

## Original Code
See `original/order_processing.py` - a class-based implementation with:
- 6 steps: validate, calculate total, process payment, reserve inventory, calculate shipping, schedule shipment
- Simulated failures (20% payment, 15% inventory, 10% shipping)
- **Naive retry logic built into the functions** (which we remove and replace with Temporal's automatic retries)
- No compensation logic (payment/inventory succeed but shipping fails = orphaned data)

## Learning Goals
- Handle more complex state across multiple activities
- Remove naive retry logic and trust Temporal's retry policies
- Work with external state (inventory database, payment tracking)
- Understand when calculations should be workflow logic vs activities
- Debug complex Temporal errors systematically

## Key Concepts
- **Multiple Activities**: 5 activities orchestrated in sequence
- **External State**: Inventory and payment systems are external to workflows
- **Retry Policies**: Each activity has configurable retry behavior
- **State Passing**: Order data and generated IDs flow between activities
- **Compensation Need**: This exercise exposes the need for sagas/compensations (not implemented yet)

## Running the Exercise

### Prerequisites
**IMPORTANT**: Use Python 3.9 or 3.10, NOT Python 3.14!

Python 3.14 has compatibility issues with the Temporal SDK that cause cryptic `os.stat` errors. If you encounter mysterious sandbox violations, check your Python version first.

1. **Start Temporal**:
   ```bash
   temporal server start-dev
   ```

2. **Create virtual environment with correct Python version**:
   ```bash
   # Check your Python version first
   python --version
   
   # If you have Python 3.14, use 3.9 instead:
   python3.9 -m venv venv
   # OR if only one Python installed, ensure it's 3.9 or 3.10
   
   source venv/bin/activate  # macOS/Linux
   # OR
   venv\Scripts\activate  # Windows
   ```

3. **Install dependencies**:
   ```bash
   cd python
   pip install -r requirements.txt
   ```

4. **Run the worker** (Terminal 1):
   ```bash
   python worker.py
   ```

5. **Run the client** (Terminal 2):
   ```bash
   python client.py
   ```

6. **Check Temporal UI**: http://localhost:8233

## Common Issues & Solutions

### Issue #1: `RestrictedWorkflowAccessError: Cannot access os.stat.__call__`

**Symptom**: Cryptic error about `os.stat` with huge stack trace, doesn't point to your code.

**Cause**: You're importing non-deterministic modules (`time`, `random`, `datetime.datetime`) at the top of `workflow.py`.

**Solution**:
```python
# ❌ WRONG - Don't import these in workflow.py
import time
import random
from datetime import datetime

# ✅ CORRECT - Only import these in activities.py
# In workflow.py, only import:
from datetime import timedelta  # This is OK (deterministic)
from temporalio import workflow
from models import Order, OrderResult

# Wrap activity imports
with workflow.unsafe.imports_passed_through():
    from activities import (...)
```

**Why**: Temporal workflows must be deterministic. Importing `time` or `random` at the module level violates this, even if you don't use them.

### Issue #2: `TypeError: Object of type Decimal is not JSON serializable`

**Symptom**: Error when starting workflow, can't serialize `Decimal` type.

**Cause**: Temporal's default JSON converter doesn't handle Python's `Decimal` type.

**Solution**: Use `float` instead of `Decimal` for prices and amounts in your dataclasses:
```python
@dataclass
class Item:
    sku: str
    quantity: int
    price: float  # ✅ Use float, not Decimal
```

**Note**: In production, you'd want `Decimal` for financial precision and would implement a custom data converter. For learning exercises, `float` is fine.

### Issue #3: Python 3.14 Compatibility

**Symptom**: Mysterious errors that don't make sense, `os.stat` errors, sandbox violations.

**Cause**: Temporal Python SDK is tested primarily on Python 3.9-3.11. Python 3.14 is too new.

**Solution**: 
```bash
# Delete existing venv
rm -rf venv

# Create new venv with Python 3.9
python3.9 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### Issue #4: Cached Imports (`__pycache__`)

**Symptom**: You fixed the code but still getting the same error.

**Cause**: Python caches compiled bytecode. Old imports stick around.

**Solution**:
```bash
# Kill your worker (Ctrl+C)

# Delete all cache
find . -type d -name "__pycache__" -exec rm -r {} +
# OR manually:
rm -rf __pycache__
rm -rf */__pycache__

# Restart worker
python worker.py
```

### Issue #5: Missing `await` on activity calls

**Symptom**: Workflow completes instantly, activities never execute, or weird type errors.

**Cause**: Forgot `await` keyword when calling `workflow.execute_activity()`.

**Solution**:
```python
# ❌ WRONG
result = workflow.execute_activity(...)

# ✅ CORRECT
result = await workflow.execute_activity(...)
```

### Issue #6: Inventory Database Logic

**Symptom**: `TypeError: 'Item' object is not iterable` or comparison errors.

**Cause**: Inventory database stores `Item` objects but trying to compare/use them as integers.

**Solution**:
```python
# Store Item objects
self._inventory: Dict[str, Item] = {...}

# Access quantity property
def check_availability(self, sku: str, quantity: int) -> bool:
    item = self._inventory.get(sku)
    if item is None:
        return False
    return item.quantity >= quantity  # ✅ Compare item.quantity
```

## What to Observe

1. **Sequential Execution**: Activities run in order: validate → calculate → payment → inventory → shipping
2. **Automatic Retries**: Watch activities fail and retry automatically (check worker logs)
3. **Retry Attempts**: Each activity tries up to 3 times before giving up
4. **Temporal UI**: Shows complete execution history, including retry attempts
5. **Simulated Failures**: 
   - Payment fails ~20% of the time
   - Inventory fails ~15% of the time  
   - Shipping fails ~10% of the time

## Architecture Notes

### What's an Activity vs Workflow Logic?

**Activities (Non-Deterministic)**:
- `validate_order` - Could call external validation service
- `process_payment` - Calls payment gateway
- `reserve_inventory` - Modifies external database
- `schedule_shipment` - Calls shipping provider API

**Could be Workflow Logic (Deterministic)**:
- `calculate_total_with_shipping` - Pure math based on inputs
  - Currently an activity, but could be moved to workflow
  - No external calls, no randomness, just calculations

**Rule of Thumb**: If it's pure logic with no side effects, it can be workflow code. If it touches external systems or has randomness, make it an activity.

### The Compensation Problem

This workflow has a critical issue: **no rollback/compensation logic**.

**Scenario**:
1. ✅ Payment succeeds ($100 charged)
2. ✅ Inventory reserved (5 units allocated)
3. ❌ Shipping fails (all 3 retry attempts exhausted)
4. **Result**: Customer charged, inventory locked, but order never ships!

**Solution** (coming in future exercises): Implement compensating activities:
- `refund_payment()` - If shipping fails
- `release_inventory()` - If payment or shipping fails

This is the **Saga pattern**, which we'll explore in Week 3-4.

## Time Estimate
~3 hours (including debugging)

**Why longer than Exercise #1?**
- More complex state management
- Python version compatibility issues
- Temporal sandbox restrictions are less forgiving
- Multiple activities with different failure modes
- Real debugging experience (not just following happy path)

**This is realistic** - production Temporal development involves working through these issues.

## Debugging Tips for Future Exercises

1. **Always check Python version first**: `python --version`
2. **Delete `__pycache__` when things don't make sense**
3. **Use Python 3.9 or 3.10** for Temporal projects
4. **Read error messages from bottom up** - the actual error is usually at the end
5. **Look for YOUR filenames** in stack traces - ignore Temporal internals
6. **Add debug prints liberally** - `print(f"DEBUG: {variable}")` is your friend
7. **Test incrementally** - don't write all 5 activities before testing
8. **Use `float` not `Decimal`** unless you implement custom converter
9. **Never import `time`, `random`, `datetime.now()` in workflow.py**

## Systematic Debugging Approach

When you hit errors, follow this process:

1. **Identify error type**:
   - `RestrictedWorkflowAccessError` → Check workflow.py imports
   - `TypeError: not JSON serializable` → Check dataclass types
   - `TypeError: not iterable` → Check if you're calling methods vs accessing properties
   - `Missing await` → Look for activity calls without await

2. **Isolate the problem**:
   - Create a minimal test workflow with just ONE activity
   - If test works, problem is in your main workflow
   - If test fails, problem is in imports or setup

3. **Clear caches**:
   - Delete `__pycache__` directories
   - Restart worker completely
   - Verify Python version

4. **Add debug output**:
   - Print types: `print(f"type: {type(variable)}")`
   - Print values: `print(f"value: {variable}")`
   - Add prints before/after each activity call

5. **Check Temporal UI**:
   - Look at workflow history
   - See which activity failed
   - Check retry attempts
   - Read error messages there (often clearer than logs)

## Next Steps
Exercise #3 will introduce:
- Compensation patterns (sagas)
- More realistic "messy" starting code
- Parallel activity execution (fan-out/fan-in)

---

**Status**: Complete ✅  
**Language**: Python  
**Concepts**: Multiple activities, external state, retry policies, debugging strategies  
**Difficulty**: Medium (2/5)  
**Actual Time**: ~3 hours
