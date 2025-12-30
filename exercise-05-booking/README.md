# Exercise 05 - Travel Booking Saga

## Overview

This exercise teaches the **SAGA pattern** with **compensating transactions** - one of the most important patterns for building reliable distributed systems with Temporal.

### The Problem

When booking a complete travel package (flight + hotel + car + payment), what happens if the car rental service fails **after** you've already booked and charged for the flight and hotel?

Without compensations:
```
✅ Flight booked ($280)
✅ Hotel booked ($945) 
❌ Car rental FAILED
Result: Customer charged $1,225 for a trip they can't take!
```

With compensations (SAGA pattern):
```
✅ Flight booked ($280)
✅ Hotel booked ($945)
❌ Car rental FAILED
   ↓ Compensation chain triggers
✅ Hotel cancelled (room released)
✅ Flight cancelled (seat released)
✅ Payment refunded
Result: Customer not charged, all resources released!
```

## What You'll Learn

1. **Implementing compensating transactions** - Rollback operations for distributed systems
2. **Saga orchestration** - Managing the order of compensations (reverse order!)
3. **Distinguishing critical vs auxiliary failures** - When to compensate and when not to
4. **State tracking in workflows** - Knowing what succeeded so you know what to compensate
5. **Activity design for compensation** - Creating symmetric book/cancel operations

## File Structure

```
exercise-05-booking/
├── README.md                      # This file
│
├── solution/                      # Folder for solution
│   ├── database.py                # Data models and booking database
│   ├── seed_data_loader.py        # Loads airports, flights, hotels, cars
│   ├── travel-seed-data.json      # Seed data for 12 airports, routes, hotels
│   │
│   ├── activities.py              # Booking + Compensation activities
│   ├── workflow.py                # Saga orchestration workflow
│   ├── worker.py                  # Temporal worker
│   ├── client.py                  # Test client (high-volume, 43 bookings)
│   │
│   └── docker-compose.yml         # Temporal server (if needed)
│
└── original/                      # Simplified pre-Temporal version
    ├── models.py                  # Simple dataclasses
    └── pre-temporal.py            # Messy code demonstrating the saga problem

```

## Prerequisites

- Python 3.9+
- Temporal server running (local or Temporal Cloud)
- Dependencies: `temporalio`, `dataclasses`, `typing`

## Setup

### 1. Install Dependencies

```bash
pip install temporalio
```

### 2. Start Temporal Server
This starts the CLI version of Temporal, or you can use Docker

```bash
temporal server start-dev
```

Temporal UI will be available at: http://localhost:8233

### 3. Verify Seed Data

The database is automatically seeded when the worker starts:
- **12 airports** (SFO, LAX, JFK, ORD, MIA, etc.)
- **12 flight routes** (with economy/business/first class)
- **720 flight instances** (60 days starting from 2025-01-20)
- **8 hotels** (standard/deluxe/suite rooms)
- **32 rental cars** (economy/compact/midsize/suv/luxury)

## Running the Exercise

### Step 1: Start the Worker

In one terminal:
```bash
python worker.py
```

You should see:
```
Database initialized with:
  12 airports
  12 flight routes
  720 flight instances
  8 hotels
  60 hotel rooms
  32 rental cars
✓ Booking database initialized
Worker started, ctrl+c to exit
```

### Step 2: Run the Test Client

In another terminal:
```bash
python client.py
```

This runs **44 test bookings**:
- **30 valid bookings** - Will succeed OR fail randomly due to:
  - 25% hotel service failures
  - 40% car rental failures
  - 10% payment failures
- **3 flight failures** - No compensation needed (fail at step 1)
- **5 hotel failures** - Compensate: cancel_flight
- **5 car failures** - Compensate: cancel_hotel + cancel_flight
- **1 payment failure** - Compensate: cancel_car + cancel_hotel + cancel_flight (FULL CHAIN)

### Step 3: Observe Results

**Console Output:**
```
[ 1/44] Customer-001         | San Francisco   → New York        | 2025-01-20  ✅ SUCCESS
[ 2/44] Customer-002         | Los Angeles     → Miami           | 2025-01-22  ❌ FAILED
...
[31/44] Presidential-Suite   | San Francisco   → New York        | 2025-01-25  ❌ FAILED
...
[36/44] Limousine-Fail       | Los Angeles     → Miami           | 2025-02-04  ❌ FAILED
...
[44/44] PaymentFail-Test     | San Francisco   → New York        | 2025-02-19  ❌ FAILED

RESULTS:
  ✅ Successful bookings: ~15-18
  ❌ Failed bookings: ~26-29
```

**Temporal UI (http://localhost:8233):**

1. Click on any failed workflow (look for names like `booking-Presidential-Suite-...`)
2. View the **Event History** tab
3. You should see the compensation chain:
   ```
   ✅ ActivityTaskScheduled: store_booking_request
   ✅ ActivityTaskCompleted: store_booking_request
   ✅ ActivityTaskScheduled: book_flight
   ✅ ActivityTaskCompleted: book_flight ($280)
   ✅ ActivityTaskScheduled: book_hotel
   ❌ ActivityTaskFailed: book_hotel (No available presidential rooms)
      ↓ Workflow enters compensation
   ✅ ActivityTaskScheduled: cancel_flight
   ✅ ActivityTaskCompleted: cancel_flight
   ❌ WorkflowExecutionFailed
   ```

## Key Implementation Details

### Compensation Order

Compensations **must** run in reverse order:
```
# Forward (booking):
flight → hotel → car → payment

# Reverse (compensation):
payment → car → hotel → flight
```

### State Tracking

The workflow tracks what succeeded:
```python
flight_result = None
hotel_result = None
car_result = None
payment_result = None

try:
    flight_result = await workflow.execute_activity(book_flight, ...)
    hotel_result = await workflow.execute_activity(book_hotel, ...)
    # ...
except ActivityError as e:
    # Only compensate what succeeded
    if car_result:
        await workflow.execute_activity(cancel_car, ...)
    if hotel_result:
        await workflow.execute_activity(cancel_hotel, ...)
    if flight_result:
        await workflow.execute_activity(cancel_flight, ...)
    raise
```

### Activity Design

Each booking activity has a corresponding cancellation:

**Booking Activities:**
- `store_booking_request` - Store customer request
- `book_flight` - Reserve seat, create reservation
- `book_hotel` - Reserve room, create reservation
- `book_car` - Reserve vehicle, create reservation
- `accept_payment` - Process payment

**Compensation Activities:**
- `cancel_flight` - Release seat, remove reservation
- `cancel_hotel` - Release room dates, remove reservation
- `cancel_car` - Release car dates, remove reservation
- `refund_payment` - Process refund

### When NOT to Compensate

Email confirmation can fail, but we **don't compensate** for it:
```python
# Payment succeeded - booking is complete
payment_result = await workflow.execute_activity(accept_payment, ...)

# Email can fail but we don't care - booking is done!
# No compensation if this fails
await workflow.execute_activity(send_confirmation_email, ...)
```

This is an **auxiliary operation** - it's nice to have but not critical to the booking's validity.

### Testing Full Compensation Chain

To guarantee a payment failure (and test the full compensation chain), we use a special confirmation code pattern:

```python
# In activities.py
if "FORCE-PAYMENT-FAIL" in confirmation_code:
    raise ApplicationError("Forced payment failure for testing")

# Hotel and car activities bypass random failures for this pattern
if "FORCE-PAYMENT-FAIL" not in request.confirmation_code:
    # Only do random failures for normal bookings
    if random.random() < 0.25:  # or 0.40 for car
        raise ApplicationError("Service unavailable")
```

This allows the `PaymentFail-Test` booking to reliably reach the payment step and fail there, demonstrating the complete saga pattern.

## Testing Different Failure Scenarios

The test client includes specific scenarios:

### Flight Failures (No Compensation)
- `EarlyDate-Fail` - Requests flight before seeded date range
- `LateDate-Fail` - Requests flight after seeded date range
- `BadRoute-Fail` - Requests route with no direct flight

### Hotel Failures (Compensate: Flight)
- `Presidential-Suite` - Invalid room type (only standard/deluxe/suite exist)
- `Penthouse-Fail` - Invalid room type
- `Oceanview-Fail` - Invalid room type
- `ExecutiveSuite-Fail` - Invalid room type
- `DeluxeKing-Fail` - Invalid room type format

### Car Failures (Compensate: Hotel + Flight)
- `Limousine-Fail` - Invalid car type (only economy/compact/midsize/suv/luxury exist)
- `SportsCar-Fail` - Invalid car type
- `Convertible-Fail` - Invalid car type
- `LuxurySUV-Fail` - Invalid car type
- `Electric-Fail` - Invalid car type

### Payment Failures (Compensate: Car + Hotel + Flight)
- `PaymentFail-Test` - Forced payment failure using special confirmation code
- Uses `"FORCE-PAYMENT-FAIL"` in confirmation code to bypass random hotel/car failures
- All bookings succeed, payment fails
- **Tests the FULL compensation chain**: cancel_car → cancel_hotel → cancel_flight
- This is the complete saga pattern in action!

### Random Failures (Compensate: Varies)
- Valid bookings will randomly fail at hotel (25%), car (40%), or payment (10%)
- These demonstrate realistic service failures

## Expected Outcomes

From 44 total bookings:

**Successes (~15-18 bookings):**
- All steps complete successfully
- No compensations run
- Customer charged, reservations confirmed

**Flight Failures (3 bookings):**
- Fail immediately
- No compensations needed
- Workflow ends in failed state

**Hotel Failures (~5-8 bookings):**
- Flight succeeds, hotel fails
- Compensation: `cancel_flight`
- Flight seat released, customer not charged

**Car Failures (~5-8 bookings):**
- Flight + hotel succeed, car fails
- Compensation: `cancel_car`, `cancel_hotel`, `cancel_flight`
- All resources released, customer not charged

**Payment Failures (~3-4 bookings):**
- All bookings succeed, payment fails
- Includes 1 forced failure (`PaymentFail-Test`) + ~2-3 random failures
- Compensation: `cancel_car`, `cancel_hotel`, `cancel_flight`
- All resources released, customer not charged
- **This demonstrates the full compensation chain!**

## Learning Checkpoints

After completing this exercise, you should be able to:

- ✅ Explain when and why to use the SAGA pattern
- ✅ Implement compensating transactions for distributed operations
- ✅ Design activities with symmetric book/cancel operations
- ✅ Track workflow state to determine what needs compensation
- ✅ Order compensations correctly (reverse order of operations)
- ✅ Distinguish between critical failures (compensate) and auxiliary failures (don't compensate)
- ✅ Use Temporal's retry policies with compensations
- ✅ Debug compensation workflows using Temporal UI

## Common Pitfalls

### 1. Wrong Compensation Order
❌ **Wrong:** Compensate in same order as booking (flight → hotel → car)  
✅ **Right:** Compensate in reverse order (car → hotel → flight)

### 2. Missing State Tracking
❌ **Wrong:** Try to compensate everything in the except block  
✅ **Right:** Only compensate operations that succeeded (`if flight_result:`)

### 3. Forgetting to Store Booking Request
❌ **Wrong:** Generate confirmation_code in workflow, activities can't find booking  
✅ **Right:** Store booking request in database before calling booking activities

### 4. Not Awaiting Compensations
❌ **Wrong:** `workflow.execute_activity(cancel_flight, ...)`  
✅ **Right:** `await workflow.execute_activity(cancel_flight, ...)`

### 5. Compensating Auxiliary Failures
❌ **Wrong:** Compensate when email fails  
✅ **Right:** Email failure is not critical, don't compensate

## Next Steps

1. **Experiment with failure rates** - Adjust the random failure percentages in activities.py
2. **Add more compensation activities** - Try adding `refund_payment` compensation
3. **Implement parallel booking** - Book flight, hotel, and car in parallel (advanced!)
4. **Add timeout handling** - What happens if an activity takes too long?
5. **Test idempotency** - What happens if a compensation is retried?

## Additional Resources

- [Temporal SAGA Pattern Documentation](https://docs.temporal.io/encyclopedia/saga-pattern)
- [Activity Retry Policies](https://docs.temporal.io/encyclopedia/retry-policies)
- [Workflow Testing Best Practices](https://docs.temporal.io/develop/testing)

## Estimated Time

- **Understanding the problem:** 15 minutes
- **Implementing compensations:** 2-3 hours
- **Testing and debugging:** 1-2 hours
- **Total:** 3-5 hours

---

**🎯 Success Criteria:**

When you run the high-volume test, you should see:
- ✅ Zero "orphaned" reservations in the database
- ✅ Compensations running in reverse order in Temporal UI
- ✅ Failed workflows properly cleaning up after themselves
- ✅ Successful workflows completing without compensations