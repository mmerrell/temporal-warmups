# exercise-01-registration Java

## Run the non-temporalized example

```bash
mvn compile exec:java
```

Expected output

```bash
Starting registration for alice (alice@example.com)
============================================================

Validating user data for alice@example.com...
✓ Validation passed
Creating user record for alice...
✓ User created with ID: user_1
Sending welcome email to alice@example.com...
✓ Welcome email sent (total sent: 1)
Sending verification email to alice@example.com...
✓ Verification email sent with token: token_user_1_1767105659268

============================================================
✓ Registration complete for alice!
User ID: user_1
Verification token: token_user_1_1767105659268
============================================================


============================================================
Starting registration for bob (bob@example.com)
============================================================

Validating user data for bob@example.com...
✓ Validation passed
Creating user record for bob...

============================================================
✗ Registration failed for bob: Database connection timeout
============================================================


============================================================
Starting registration for alice (alice2@example.com)
============================================================

Validating user data for alice2@example.com...
✓ Validation passed
Creating user record for alice...
✓ User created with ID: user_2
Sending welcome email to alice2@example.com...
✓ Welcome email sent (total sent: 3)
Sending verification email to alice2@example.com...
✓ Verification email sent with token: token_user_2_1767105663894

============================================================
✓ Registration complete for alice!
User ID: user_2
Verification token: token_user_2_1767105663894
============================================================



Final Results:
Users in database: 2
Emails sent: 4

============================================================
PROBLEMS WITH THIS APPROACH:
============================================================
1. No retry logic - transient failures cause complete failures
2. No durability - if process crashes, all state is lost
3. No visibility - can't see workflow progress in a UI
4. No recovery - can't resume from failure point
5. Manual error handling - error-prone and repetitive
6. All-or-nothing - can't retry just the failed step

Temporal solves all of these problems!
============================================================

```

## Breaking down the problem

1. Separate Concerns: Workflow vs Activities

Current state: Everything is in one monolithic class.

Goal: Split into two categories:
- Activities (non-deterministic operations that can fail):
    - validateUserData() - even validation might call external services
    - createUserRecord() - database writes
    - sendWelcomeEmail() - external email API
    - sendVerificationEmail() - external email API
- Workflow (deterministic orchestration):
    - Calls activities in sequence
    - Handles the overall registration flow
    - Makes decisions based on activity results

Why: Temporal can retry activities independently. If email fails, it doesn't re-run database creation. The workflow is the durable "memory" of where you are in the process.

2. Create Activity Interface and Implementation

What: Define a Java interface (e.g., RegistrationActivities) with methods for each activity, then create an implementation class.

Why: Temporal uses interfaces for type-safe activity invocation. The interface defines the contract, the implementation contains the actual logic (database calls, API calls, etc.).

3. Create Workflow Interface and Implementation

What: Define a workflow interface (e.g., RegistrationWorkflow) with a single method (the workflow entry point), then implement the orchestration logic.

Key differences from current code:
- No direct method calls - use Activity stubs
- Pass activity options (timeouts, retry policies) when invoking
- Workflow code must be deterministic (no Random, System.currentTimeMillis() directly in workflow)

Why: The workflow becomes the durable orchestration layer. If the worker crashes, Temporal replays the workflow from history and resumes from where it left off.

4. Create the Worker

What: A separate class with a main() method that:
- Connects to Temporal server
- Registers your workflow and activity implementations
- Listens on a task queue (e.g., "registration-queue")
- Runs forever, processing tasks

Why: Workers are the execution engines. They must be running for workflows to execute. You can have multiple workers for horizontal scaling.

5. Create the Client (Starter)

What: A separate class that:
- Connects to Temporal server
- Creates a workflow stub
- Starts workflow executions with input data
- Can retrieve results

Why: This is your entry point - how you trigger registrations. The client can be a web server, CLI tool, or anything that needs to start a registration workflow.

6. Configure Retry Policies and Timeouts

What: When calling activities from the workflow, specify:
- startToCloseTimeout - max time for one activity attempt
- RetryPolicy - how many retries, backoff strategy, which exceptions to retry

Why: This is where Temporal's magic happens. Email failures? Automatic retries with exponential backoff. Database timeout? Retry up to N times. No manual retry logic needed.

7. Handle Data Serialization

What: Create simple data classes (POJOs) for workflow inputs/outputs:
- RegistrationRequest (email, username, password)
- RegistrationResult (success, userId, token, error)

Why: Temporal serializes data to JSON. Keep it simple - strings, numbers, basic types. Avoid enums unless you have custom serialization.

8. Project Structure

Reorganize into:
src/main/java/
├── workflows/
│   ├── RegistrationWorkflow.java (interface)
│   └── RegistrationWorkflowImpl.java
├── activities/
│   ├── RegistrationActivities.java (interface)
│   └── RegistrationActivitiesImpl.java
├── model/
│   ├── RegistrationRequest.java
│   └── RegistrationResult.java
├── Worker.java
└── Client.java

Why: Clear separation of concerns. Easy to test, maintain, and understand.

Key Mental Model Shift

Before: "If this fails, the whole thing fails. Start over."

After: "Each step is durable. If email fails, Temporal retries just that step. If the process crashes, Temporal resumes from the last completed activity."

The workflow becomes a state machine that Temporal manages. You write the business logic; Temporal handles durability, retries, and visibility.

What You'll Observe

- Run the worker, then run the client - registration starts
- Kill the worker mid-execution - workflow pauses
- Restart worker - workflow resumes exactly where it left off
- Go to Temporal UI (http://localhost:8233) - see the entire execution history

This is the power of durable execution: your business logic survives process crashes, network failures, and transient errors.

