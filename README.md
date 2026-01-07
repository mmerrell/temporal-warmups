# Temporal Warm-up Exercises

A progressive collection of hands-on exercises for building muscle memory with Temporal workflow orchestration. These exercises are designed for daily practice to develop fluency across Python, Java, Go, and TypeScript.

## 🎯 Purpose

This repository contains "warm-up" exercises where I take ordinary code and temporalize it—converting procedural operations into durable Temporal workflows with proper activity separation, retry policies, and error handling.

**Goal:** Complete each exercise in ~1 hour, building speed and confidence over time.

## 📚 Exercise Structure

Each exercise includes:
- **Original code**: The "before" version (messy, procedural, no durability)
- **Temporalized version**: The "after" version with proper workflow/activity separation
- **Multiple languages**: Python, Java, Go, and (eventually) TypeScript implementations
- **Progressive complexity**: Starting simple, adding real-world patterns over time

### Progression Path

**Week 1-2: Fundamentals**
- Basic workflow → activity patterns
- Simple retry policies
- Clean starting code
- Focus: Getting the pattern down

**Week 3-4: Realistic Scenarios**
- Messier starting code
- Compensation patterns (sagas)
- Partial failure handling
- Focus: Real-world challenges

**Week 5+: Advanced Patterns**
- **Signals and queries** (introduced in Exercise 06)
- Parallel execution (fan-out/fan-in)
- Parent-child workflows
- Workflow versioning
- Continue-as-new
- Focus: Production patterns

## 🗂️ Repository Structure
```
temporal-warmups/
├── exercise-01-registration/     # User registration flow
│   ├── README.md                  # Exercise description & goals
│   ├── original/                  # Starting code (before Temporal)
│   │   └── registration.py
│   ├── python/                    # Python solution
│   │   ├── models.py
│   │   ├── activities.py
│   │   ├── workflow.py
│   │   ├── worker.py
│   │   ├── client.py
│   │   └── requirements.txt
│   ├── java/                      # Java solution
│   ├── go/                        # Go solution
│   └── typescript/                # TypeScript solution
├── exercise-02-email/            # Email verification
├── exercise-02-orders/           # Order processing
├── exercise-03-hotel/            # Hotel booking with fallbacks
├── exercise-04-registration-java/ # Java-specific registration
├── exercise-05-booking/          # Travel booking (sagas)
├── exercise-06-support-triage/   # ⭐ AI support triage (SIGNALS!)
│   ├── exercise-06-README.md      # @Signal and OpenAI Java SDK
│   └── src/main/java/
│       ├── exercise/              # Pre-Temporal baseline
│       └── solution/temporal/     # Temporal solution with signals
└── README.md
```

## 🚀 Getting Started

### Prerequisites

**For all exercises:**
- Temporal server running locally
```bash
  temporal server start-dev
```
  OR
```bash
  docker-compose up
```

**Language-specific:**
- **Python**: Python 3.8+, `pip`
- **Java**: JDK 11+, Maven or Gradle
- **Go**: Go 1.21+
- **TypeScript**: Node.js 18+, npm

### Running an Exercise

Each exercise contains language-specific instructions in its README. General pattern:

1. **Start Temporal server** (if not already running)
2. **Navigate to exercise directory**
3. **Choose your language** (e.g., `cd python/`)
4. **Install dependencies** (language-specific)
5. **Run the worker** (in one terminal)
6. **Run the client** (in another terminal)
7. **Check Temporal UI** at http://localhost:8233

## 🆕 What's New in Exercise 06

**Exercise 06 - Support Triage** introduces **Signals** for the first time in this curriculum!

### New Concepts:
- **`@SignalMethod`** (Java) - Define signal handlers in workflows
- **Human-in-the-loop patterns** - Workflows pause for external approval
- **Signal timing** - Understanding when signals arrive vs when workflow reaches `await()`
- **Sending signals via CLI** - `temporal workflow signal` commands for testing
- **Multi-agent AI orchestration** - Coordinating multiple LLM API calls with Temporal
- **State tracking in workflows** - Using instance fields to track signal receipt

### Why This Matters:
Signals enable workflows to pause and wait for external events like:
- Human approvals (compliance, risk management)
- External system notifications
- User input or decisions
- Real-time updates from other services

This is a **production-critical pattern** for building reliable, auditable systems with human oversight.

## 📖 Learning Approach

### Daily Practice
- Pick an exercise
- Time yourself (aim for ~1 hour)
- Focus on one language at a time
- Review the solution afterward

### Cross-Language Learning
- Start with Python (most comfortable)
- Progress to Java (building on ecommerce experience)
- Tackle Go (new territory, steepest curve)
- Add TypeScript when ready

### Key Concepts to Master
- Workflow vs Activity boundaries
- Deterministic vs non-deterministic code
- Retry policies and error handling
- State management and durability
- Compensation patterns (sagas)
- Parallel execution patterns
- **Signals and queries** ⭐ **NEW in Exercise 06!**
  - Human-in-the-loop approvals
  - External events triggering workflow state changes
  - Sending signals via Java client code or Temporal CLI
- Workflow versioning

## 🎓 Context

These exercises are part of a larger learning journey to:
1. Build deep Temporal expertise for interviews and presentations
2. Develop workshop materials for customer on-site training
3. Create progressively complex examples for teaching distributed systems concepts

## 📝 Notes

- **Not production code**: These are learning exercises, optimized for clarity
- **Iterations expected**: Solutions may be revised as patterns evolve
- **Language parity**: Not all exercises will have all 4 languages immediately
- **Timing tracked**: Each exercise notes approximate completion time

## 🔗 Resources

- [Temporal Documentation](https://docs.temporal.io)
- [Temporal Python SDK](https://github.com/temporalio/sdk-python)
- [Temporal Java SDK](https://github.com/temporalio/sdk-java)
- [Temporal Go SDK](https://github.com/temporalio/sdk-go)
- [Temporal TypeScript SDK](https://github.com/temporalio/sdk-typescript)

## 📅 Progress Tracking

| Exercise | Python | Java | Go | TypeScript | Concepts |
|----------|--------|------|----|-----------| ---------|
| 01 - Registration | ✅ | ✅ | ⬜ | ⬜ | Basic workflow, activities, retries |
| 02 - Email Verification | ✅ | ✅ | ⬜ | ⬜ | Muscle memory building |
| 02 - Order Processing | ✅ | ⬜ | ⬜ | ⬜ | Multiple activities, external state |
| 03 - Hotel Booking | ✅ | ⬜ | ⬜ | ⬜ | Messy code refactoring, fallback patterns |
| 04 - Registration (Java) | ⬜ | ✅ | ⬜ | ⬜ | Java-specific patterns |
| 05 - Travel Booking | ✅ | ⬜ | ⬜ | ⬜ | Saga/compensation patterns |
| 06 - Support Triage | ⬜ | ✅ | ⬜ | ⬜ | **Signals (NEW!)**, human-in-the-loop, multi-agent AI |

---

**Status**: Active development (Dec 2024 - Jan 2025)
```

