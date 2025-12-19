# Temporal Warmups - Learning Curriculum

## Overview

A progressive series of hands-on exercises designed to build expertise with Temporal workflow orchestration through daily practice. Each exercise focuses on converting ordinary procedural code into durable Temporal workflows with proper activity separation, retry policies, and error handling.

**Goal:** Complete each exercise in approximately 1 hour, building speed and confidence over time across Python, Java, Go, and TypeScript.

**Brand Color:** `#AEB6D9` (Temporal's primary brand color)

---

## Week 1-2: Fundamentals

Focus on learning the basic workflow → activity pattern across multiple languages. Clean starting code, straightforward scenarios, emphasis on getting the pattern down.

### Exercise #1: User Registration ⭐⭐
**Language:** Python  
**Time:** ~1 hour  
**Concepts:** Basic workflow, activities, retries

**Scenario:** Simple 4-step user registration flow (validate, create user, send welcome email, send verification email).

**Learning Goals:**
- Separate workflow orchestration from activities
- Implement retry policies
- See automatic failure recovery
- Understand durability (workflow survives worker restarts)

**Key Takeaways:**
- Workflow vs Activity boundaries
- Deterministic vs non-deterministic code
- External state management (database)
- Basic retry policies

**Status:** ✅ Complete

---

### Exercise #2: Order Processing ⭐⭐⭐
**Language:** Python  
**Time:** ~3 hours (includes debugging)  
**Concepts:** Multiple activities, external state, debugging

**Scenario:** E-commerce order processing (validate, calculate total, process payment, reserve inventory, schedule shipment).

**Learning Goals:**
- Handle more complex state across multiple activities
- Remove naive retry logic and trust Temporal's retry policies
- Work with external state (inventory database, payment tracking)
- Debug complex Temporal errors systematically

**Key Takeaways:**
- Python version compatibility (3.9-3.10, NOT 3.14)
- Temporal's sandbox restrictions
- Dataclass serialization (use `float` not `Decimal`)
- `__pycache__` can lie to you
- How to debug cryptic `os.stat` errors

**Common Pitfalls:**
- Non-deterministic imports in workflow.py
- Missing `await` on activity calls
- Wrong Python version
- Cached imports

**Status:** ✅ Complete

---

### Exercise #2.5: Email Verification ⭐
**Language:** Python  
**Time:** 20-45 minutes  
**Concepts:** Muscle memory, speed building

**Scenario:** Two-step email verification (generate token, send email).

**Learning Goals:**
- Build muscle memory for basic pattern
- Practice setup rhythm without debugging distractions
- Get comfortable with development loop
- Speed: Target under 30 minutes

**Key Takeaways:**
- Reinforce the basic pattern through repetition
- Build confidence and speed
- Clean, minimal exercise for practice

**Status:** ✅ Complete

---

### Exercise #3: Hotel Reservation ⭐⭐⭐
**Language:** Python  
**Time:** ~1.5 hours  
**Concepts:** Messy code refactoring, first compensations

**Scenario:** Hotel booking with payment, room assignment, and confirmation (starting from messy 80+ line function).

**Learning Goals:**
- Refactor poorly organized code to Temporal patterns
- Make judgment calls about workflow vs activity boundaries
- Introduce basic compensation (refund if email fails)
- Handle inconsistent error patterns in legacy code

**Key Takeaways:**
- Breaking apart monolithic functions
- When calculations should be workflow logic vs activities
- First exposure to compensation need
- Dealing with mixed error handling approaches

**Status:** 🎯 Next up

---

### Exercise #4 (Optional): Multi-Language Rotation
**Languages:** Java or Go  
**Time:** ~1.5 hours  
**Concepts:** Same patterns, different syntax

**Scenario:** Pick Exercise #1, #2.5, or #3 and implement in Java or Go.

**Learning Goals:**
- Apply the same Temporal patterns in a different language
- Understand SDK differences
- Build cross-language fluency

**Key Differences to Learn:**
- **Java:** Interface-based design, activity stubs, builder patterns
- **Go:** Context everywhere, struct-based workflows, channels

---

## Week 3-4: Real-World Patterns

Introduce complexity through realistic business scenarios and advanced Temporal features. Starting code becomes messier, patterns more sophisticated.

### Exercise #5: Saga/Compensation Patterns ⭐⭐⭐⭐
**Language:** Python  
**Time:** ~2 hours  
**Concepts:** Distributed transactions, rollback logic

**Scenario:** Multi-step booking flow (flight + hotel + car rental) where any step can fail after others succeed.

**Learning Goals:**
- Implement proper compensation logic
- Handle partial success scenarios
- Build saga pattern from scratch
- Understand when to compensate vs when to retry

**Key Patterns:**
- Forward recovery (retry until success)
- Backward recovery (compensate/rollback)
- Compensation ordering (reverse of execution order)
- Idempotent compensating activities

---

### Exercise #6: Parallel Execution (Fan-out/Fan-in) ⭐⭐⭐⭐
**Language:** Python  
**Time:** ~2 hours  
**Concepts:** Concurrent activities, partial failures

**Scenario:** Process batch of orders simultaneously, aggregate results, handle partial failures.

**Learning Goals:**
- Execute activities in parallel
- Wait for all/any to complete
- Handle scenario where 2 of 5 parallel activities fail
- Aggregate results from concurrent execution

**Key Patterns:**
- `asyncio.gather()` in Python
- `Async.function()` in Java
- `workflow.Go()` in Go
- Cancellation scopes for cleanup
- Partial success handling

---

### Exercise #7: Signals & Queries ⭐⭐⭐
**Language:** Python  
**Time:** ~1.5 hours  
**Concepts:** Interactive workflows, external communication

**Scenario:** Long-running approval workflow that can be paused, resumed, approved, or rejected from outside.

**Learning Goals:**
- Send signals to running workflows
- Query workflow state without affecting execution
- Implement pause/resume functionality
- Handle human-in-the-loop scenarios

**Key Patterns:**
- Signal handlers
- Query methods
- Workflow state management
- External interaction patterns

---

### Exercise #8: Parent-Child Workflows ⭐⭐⭐⭐
**Language:** Python  
**Time:** ~2 hours  
**Concepts:** Workflow composition, state across boundaries

**Scenario:** Order processing parent that spawns child workflows for each item, aggregates results.

**Learning Goals:**
- Start child workflows from parent
- Pass state between parent and child
- Signal children from parent
- Handle child workflow failures
- Maintain pause/resume across workflow boundaries

**Key Patterns:**
- Child workflow execution
- Parent-child communication via signals
- State aggregation
- Cascading cancellation

---

## Week 5+: Advanced & Production Patterns

Focus on debugging, optimization, versioning, and handling complex real-world scenarios.

### Exercise #9: Code Review from Hell ⭐⭐⭐⭐⭐
**Language:** Python  
**Time:** 2-3 hours  
**Concepts:** Anti-patterns, refactoring, best practices

**Scenario:** Inherited "working" workflow with terrible practices - technically runs but violates everything.

**Bad Patterns to Identify:**
- ❌ Non-deterministic code in workflow (`time.time()`, `random.random()`)
- ❌ Database calls directly in workflow code
- ❌ No retry policies configured
- ❌ Mega-activities doing too much
- ❌ Mutable workflow state that doesn't persist
- ❌ Using `time.sleep()` instead of `workflow.sleep()`
- ❌ No error handling
- ❌ Excessive activity timeouts (1 hour!)
- ❌ Non-idempotent workflow IDs
- ❌ Wrong imports at workflow level
- ❌ Using `datetime.now()` instead of `workflow.now()`

**Learning Goals:**
- Recognize anti-patterns in real code
- Understand WHY each pattern is problematic
- Practice refactoring to best practices
- Build code review checklist mindset

**Deliverable:** Refactored code + documented list of fixes with explanations

---

### Exercise #10: The Broken Workflow ⭐⭐⭐⭐⭐
**Language:** Python  
**Time:** 2-3 hours  
**Concepts:** Debugging, troubleshooting, production issues

**Scenario:** Contractor built a payment workflow, said it works, disappeared. Now it's failing in production and you're on-call.

**Hidden Problems:**
- ❌ Activities registered with wrong names in worker
- ❌ Activity signatures don't match workflow calls
- ❌ Workflow imports outside `unsafe.imports_passed_through()`
- ❌ Client passes wrong argument types
- ❌ Missing `await` on critical activity
- ❌ Task queue name mismatch
- ❌ Retry policy configured but never applied
- ❌ Non-serializable return object
- ❌ Activity references non-existent state
- ❌ Hardcoded database connection is wrong

**Learning Goals:**
- Debug systematically using error messages
- Interpret Temporal error messages
- Use Temporal UI for debugging
- Understand common integration mistakes
- Build troubleshooting muscle memory

**Realistic Touch:** Cascading errors - some only appear after fixing previous ones

**Deliverable:** Working workflow + debugging process documentation

---

### Exercise #11: Workflow Versioning ⭐⭐⭐⭐
**Language:** Python  
**Time:** ~2 hours  
**Concepts:** Safe deployments, backwards compatibility

**Scenario:** Production workflow needs a new activity added in the middle. Must support both old (in-flight) and new workflows.

**Learning Goals:**
- Use `workflow.patch()` for versioning
- Deploy changes without breaking in-flight workflows
- Understand determinism implications
- Test version compatibility

**Key Patterns:**
- Patching workflows
- Version markers
- Safe rollout strategies
- Testing multiple versions

---

### Exercise #12: Continue-as-New ⭐⭐⭐⭐
**Language:** Python  
**Time:** ~2 hours  
**Concepts:** Long-running workflows, history limits

**Scenario:** Subscription billing workflow that runs monthly for years. Must avoid history size limits.

**Learning Goals:**
- Implement continue-as-new pattern
- Understand when and why to reset workflow history
- Preserve necessary state across continuations
- Handle timing/scheduling considerations

**Key Patterns:**
- `workflow.continue_as_new()`
- State preservation
- History management
- Cron workflows vs continue-as-new

---

### Exercise #13: Complex Multi-Service Orchestration ⭐⭐⭐⭐⭐
**Language:** Python (or multi-language)  
**Time:** 3-4 hours  
**Concepts:** Everything combined

**Scenario:** E-commerce checkout that coordinates: payment gateway, inventory system, shipping provider, notification service, fraud detection, loyalty points - with compensations, parallelization, and human approvals.

**Learning Goals:**
- Combine all learned patterns
- Handle real-world complexity
- Make architectural decisions
- Optimize for performance and reliability

**Patterns Used:**
- Parallel execution
- Sagas/compensations
- Signals/queries
- Parent-child workflows
- Proper error handling
- Retry strategies
- Timeout tuning

---

## Cross-Language Exercises

### Java Track
- Exercise #1 (Registration) in Java
- Exercise #2.5 (Email Verification) in Java
- Exercise #6 (Parallel Execution) in Java - learn `Async.function()`

**Key Java Patterns:**
- Interface-based design
- Activity stubs
- Builder patterns for options
- CompletableFuture patterns

### Go Track
- Exercise #1 (Registration) in Go
- Exercise #2.5 (Email Verification) in Go
- Exercise #7 (Signals/Queries) in Go

**Key Go Patterns:**
- Context management
- Struct-based workflows
- Channels and goroutines
- Error handling patterns

### TypeScript Track
- Exercise #1 (Registration) in TypeScript
- Exercise #2.5 (Email Verification) in TypeScript
- Exercise #8 (Parent-Child) in TypeScript

**Key TypeScript Patterns:**
- Promise-based async
- Type definitions
- Worker configuration
- Module system

---

## Progression Philosophy

### Complexity Vectors

**Week 1-2 (Beginner):**
- ✅ Clean starting code
- ✅ Add ONE concept per exercise
- ✅ Focus on fundamentals
- ✅ Build muscle memory

**Week 3-4 (Intermediate):**
- ✅ Introduce messiness
- ✅ Real-world patterns (sagas, parallel, signals)
- ✅ Combine multiple concepts
- ✅ Realistic failure scenarios

**Week 5+ (Advanced):**
- ✅ Production patterns
- ✅ Debugging challenges
- ✅ Optimization
- ✅ Everything combined

### Code Messy-ness Scale

1. **Clean** - Well-organized, clear separation, easy to understand
2. **Slightly Messy** - Long functions, some mixed concerns
3. **Moderately Messy** - Nested logic, inconsistent patterns, some spaghetti
4. **Very Messy** - 100+ line functions, multiple responsibilities, hard to follow
5. **Realistic Legacy** - Multiple files, unclear architecture, anti-patterns everywhere

**Progression:** Start at 1, gradually increase to 5 by Week 5.

---

## Workshop Application

These exercises form the foundation for two workshop tracks:

### Beginner Workshop (6 hours)
**Session 1:** Foundations (Exercise #1 concepts)  
**Session 2:** Handling Failure (Exercise #2 concepts + light compensations)  
**Session 3:** Advanced Orchestration (Exercise #6 - parallel execution)  
**Session 4:** Production Patterns (Signals/Queries + versioning basics)

### Advanced Workshop (6 hours)
**Session 1:** Deep Dive on Workflow Design (Best practices, anti-patterns)  
**Session 2:** Testing Strategies (Unit, integration, replay tests)  
**Session 3:** Complex State Management (Parent-child, sagas)  
**Session 4:** Production Readiness (Exercise #9 & #10 concepts, debugging)

---

## Success Metrics

**Speed Targets:**
- Exercise #1 (first time): 60 minutes → (experienced): 20 minutes
- Exercise #2.5 (first time): 45 minutes → (experienced): 15 minutes
- Complex exercises: 2-3 hours → (experienced): 1-1.5 hours

**Skill Markers:**
- ✅ Can identify workflow vs activity boundaries instantly
- ✅ Recognizes anti-patterns in code review
- ✅ Debugs Temporal errors systematically
- ✅ Implements compensations correctly
- ✅ Comfortable across Python, Java, Go
- ✅ Ready to design production workflows

---

## Resources & References

**Temporal Documentation:**
- https://docs.temporal.io
- https://docs.temporal.io/dev-guide/python
- https://docs.temporal.io/dev-guide/java
- https://docs.temporal.io/dev-guide/go
- https://docs.temporal.io/dev-guide/typescript

**SDK Repositories:**
- Python: https://github.com/temporalio/sdk-python
- Java: https://github.com/temporalio/sdk-java
- Go: https://github.com/temporalio/sdk-go
- TypeScript: https://github.com/temporalio/sdk-typescript

**Best Practices:**
- Use Python 3.9-3.10 (NOT 3.14)
- Delete `__pycache__` when debugging
- Use `float` for money (or implement custom converter)
- Never import `time`, `random`, `datetime.now()` in workflows
- Always wrap activity imports in `unsafe.imports_passed_through()`
- Keep workflow logic deterministic
- Put all I/O and external calls in activities

---

**Status:** Active Development (December 2024 - January 2025)  
**Current Progress:** Exercises #1, #2, #2.5 Complete  
**Next Up:** Exercise #3 (Hotel Reservation)
