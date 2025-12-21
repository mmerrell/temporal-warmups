# Exercise 03 - Hotel Reservation System

## Scenario
A hotel reservation workflow that handles room availability checking, payment processing, room assignment, and guest notifications. This exercise focuses on refactoring messy, monolithic code into clean Temporal workflows while introducing fallback patterns and realistic failure scenarios.

## Original Code
See `original/hotel_reservation.py` - a class-based implementation with:
- 100+ line monolithic function doing everything
- Validation mixed with business logic
- Inconsistent retry patterns (manual retries for payment, none for room assignment, basic try/catch for email)
- Price calculation buried in the flow
- **Critical issue:** If email fails after payment/room assignment, customer is charged but not notified

## Learning Goals
- Refactor poorly organized code to Temporal patterns
- Distinguish deterministic workflow logic from non-deterministic activities
- Implement fallback notification patterns (email → SMS → manual)
- Understand when NOT to compensate (auxiliary failures vs business-critical failures)
- Handle realistic failure scenarios across multiple external services

## Key Concepts
- **Workflow Logic vs Activities**: Price calculation stays in workflow (deterministic), external calls become activities
- **Fallback Patterns**: Progressive degradation through multiple notification channels
- **Realistic Failures**: Different failure rates for different services (20% payment, 15% room assignment, 10% notifications)
- **Smart Compensation**: Don't refund valid reservations just because email failed
- **Type Serialization**: Using strings and floats instead of Enums/Decimals/datetime objects for Temporal compatibility

## Running the Exercise

### Prerequisites
**Use Python 3.9 or 3.10** (NOT Python 3.14)

1. **Start Temporal**:
   ```bash
   temporal server start-dev
   ```

2. **Create virtual environment**:
   ```bash
   python3.9 -m venv venv
   source venv/bin/activate  # macOS/Linux
   pip install -r requirements.txt
   ```

3. **Run the worker** (Terminal 1):
   ```bash
   python worker.py
   ```

4. **Run the client** (Terminal 2):
   ```bash
   python client.py
   ```

5. **Check Temporal UI**: http://localhost:8233

## File Structure

```
exercise-03-hotel/
├── original/
│   └── hotel_reservation.py    # Messy starting code
├── python/
│   ├── database.py              # Data models and HotelData class
│   ├── activities.py            # 6 activities (room check, payment, assignment, notifications)
│   ├── workflow.py              # HotelReservationWorkflow
│   ├── worker.py                # Worker setup
│   ├── client.py                # Test client with 3 sample reservations
│   └── requirements.txt
└── README.md
```

## What to Observe

1. **Deterministic Workflow Logic**:
   - Nights calculation: `(check_out - check_in).days`
   - Price calculation with 7+ night discount
   - Input validation
   - All done in workflow, no activities needed!

2. **Activity Execution**:
   - Room availability check
   - Payment processing (20% failure rate - watch retries!)
   - Room assignment (15% failure rate)
   - Notification cascade (email → SMS → front desk)

3. **Fallback Pattern in Action**:
   - Email fails (10% chance) → Try SMS
   - SMS fails (10% chance) → Alert front desk for manual call
   - Front desk notification always succeeds (no failures)

4. **Automatic Retries**: Each activity configured with retry policy (3 attempts, exponential backoff)

5. **Temporal UI Features**:
   - See workflow execution timeline
   - View retry attempts when activities fail
   - Inspect activity inputs/outputs
   - Check workflow history size

## Architecture Decisions

### **What Stayed in Workflow (Deterministic)**

```python
# Pure calculations - no external calls
nights = (check_out_date - check_in_date).days
base_price = ROOM_RATES[room_type]
total_price = base_price * nights

# Apply 7+ night discount
if nights >= 7:
    total_price *= 0.9

# Input validation
if not guest_name or len(guest_name) < 2:
    raise ValueError("Invalid guest name")
```

**Why workflow?**
- Pure logic based on inputs
- No I/O, no external calls
- Deterministic (same inputs = same outputs)
- Fast (no network latency)

### **What Became Activities (Non-Deterministic)**

```python
# External system interactions
check_room_availability()  # Reads from inventory
collect_payment()          # Calls payment gateway
assign_room()              # Modifies inventory
send_email_notification()  # External email service
send_sms_notification()    # External SMS service
front_desk_confirmation()  # Internal queue/system
```

**Why activities?**
- External system calls
- Can fail (network, service down, etc.)
- Non-deterministic (payment gateway might timeout)
- Need retries and timeout management

### **The Fallback Pattern**

```python
try:
    # Try primary notification channel
    await workflow.execute_activity(send_email_notification, ...)
except ActivityError:
    try:
        # Fallback to secondary channel
        await workflow.execute_activity(send_sms_notification, ...)
    except ActivityError:
        # Ultimate fallback - manual intervention
        await workflow.execute_activity(front_desk_confirmation, ...)
```

**Key insight:** Email/SMS failures are **auxiliary failures**, not business-critical. The reservation is valid even if notifications fail. Don't refund the customer!

### **When NOT to Compensate**

**Scenario:** Email fails after payment succeeds and room is assigned.

**Wrong approach:** Refund payment and release room  
**Right approach:** Log warning, try alternate notification, continue

**Why?**
- Customer successfully paid
- Room is successfully assigned
- Valid reservation exists
- Email is just notification, not core business

**When to compensate:** If a business-critical step fails (payment succeeded but external hotel PMS registration failed), THEN compensate.

## Data Modeling Decisions

### **Type Serialization Choices**

**Original attempt:**
```python
room_type: RoomTypes  # Enum - doesn't serialize!
check_in: datetime    # datetime object - doesn't serialize!
```

**Final solution:**
```python
room_type: str        # ✅ "standard", "deluxe", "suite"
check_in: str         # ✅ "2024-12-20" (ISO 8601)
```

**Trade-offs:**
- ✅ Works with Temporal's default JSON serializer
- ✅ Simple, no custom converters needed
- ❌ Lost type safety (can pass invalid room type)
- ❌ Must parse dates when calculating: `datetime.fromisoformat(check_in)`

**Future enhancement:** Custom data converter to support Enums and datetime objects (Week 5+ topic)

### **Room Availability - Known Simplification**

```python
# Current: Boolean flag (naive)
is_available: bool  # True/False for "today"

# Reality: Would need date ranges
reservations: List[Tuple[str, str]]  # [(check_in, check_out), ...]
```

**Why simplified?** Focus is on Temporal patterns, not building a hotel PMS. The naive approach works fine for learning workflow orchestration.

## Common Issues & Solutions

### **Issue #1: Enum/datetime Serialization Errors**

**Symptom:** `TypeError: Object of type RoomTypes is not JSON serializable`

**Cause:** Temporal's default JSON converter doesn't handle Python Enums or datetime objects.

**Solution:** Use strings for room types and ISO date strings for dates.

### **Issue #2: Using `workflow.uuid4()` in Client**

**Symptom:** `AttributeError: module 'temporalio.workflow' has no attribute 'uuid4'`

**Cause:** `workflow.uuid4()` only works inside workflow code, not in client code.

**Solution:**
```python
# In client.py - use standard library
import uuid
id = f"RES-{uuid.uuid4()}"

# In workflow.py - use workflow module
id = f"RES-{workflow.uuid4().hex[:8]}"
```

### **Issue #3: `activity.logger()` vs `activity.logger.info()`**

**Symptom:** `TypeError: 'LoggerAdapter' object is not callable`

**Cause:** Trying to call logger as a function instead of using its methods.

**Solution:**
```python
# ❌ Wrong
activity.logger(f"Message")

# ✅ Correct
activity.logger.info(f"Message")
```

### **Issue #4: ISO Date Format Requirements**

**Symptom:** `ValueError: Invalid isoformat string: '2025-7-12'`

**Cause:** ISO 8601 requires zero-padded months and days.

**Solution:**
```python
# ❌ Wrong
check_in = '2025-7-12'

# ✅ Correct
check_in = '2025-07-12'
```

## Complexity Progression

**Compared to Exercise #2:**
- ✅ More messy starting code (100+ line monolithic function)
- ✅ More ambiguous requirements (what should be workflow vs activity?)
- ✅ Introduction of fallback patterns
- ✅ More realistic "audit" of business logic

**Why this took ~3 hours:**
- Analyzing messy code to identify activity boundaries
- Thinking through reservation logic (availability, pricing, discounts)
- Understanding when to compensate vs when to continue
- Type serialization debugging (Enum/datetime issues)
- Natural "requirements audit" that happens during refactoring

**Key learning:** Converting to Temporal forces architectural clarity. You must decide:
- What's deterministic vs non-deterministic?
- What's core business logic vs external dependencies?
- Where are the failure boundaries?
- What failures are critical vs auxiliary?

## Time Estimate
~3 hours (including analysis and data modeling decisions)

**Why longer than Exercise #1 & #2.5?**
- More complex starting code (messy, monolithic)
- Architectural decisions (workflow vs activity boundaries)
- Business logic analysis (pricing, discounts, availability)
- Fallback pattern implementation
- Type serialization issues to work through

**This is realistic** - production Temporal adoption involves this kind of analysis and refactoring.

## What's NOT Covered (Future Exercises)

**Compensations (Week 3-4):**
- This exercise shows when NOT to compensate (email failure)
- Future exercises will show proper saga patterns with rollback
- Example: Refund payment if external PMS registration fails

**Custom Data Converters (Week 5+):**
- Support for Enums, Decimals, custom types
- Encryption/compression of payloads
- Claim check pattern for large data

**Advanced Patterns:**
- Parent-child workflows
- Signals for external updates
- Continue-as-new for long-running reservations
- Workflow versioning for production deployments

## Next Steps

**Exercise #4 (Coming Soon):**
- More complex compensations (saga pattern)
- Parallel execution patterns
- Combining multiple Temporal features

**Or continue language rotation:**
- Implement Exercise #1 or #2.5 in Java/Go to practice same patterns in different syntax

---

**Status**: Complete ✅  
**Language**: Python  
**Concepts**: Refactoring messy code, workflow vs activity decisions, fallback patterns, type serialization  
**Difficulty**: Medium-Hard (3/5)  
**Actual Time**: ~3 hours
