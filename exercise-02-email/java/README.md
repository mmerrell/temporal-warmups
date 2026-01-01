# Exercise 02 - Email Verification (Java)

## Scenario

You have a simple two-step email verification workflow:
1. Generate a unique verification token
2. Send an email with that token

This is a **muscle memory builder** - practice the basic Temporal workflow pattern without the complexity of databases or multiple data models. If you completed Exercise 01, you should be able to complete this in 45 minutes or less.

## Run the Pre-Temporal Baseline

```bash
mvn compile exec:java
```

Watch what happens when the email service fails (10% random failure rate). The entire verification fails - no retries, no recovery. The process is fragile.

Expected output shows:
- Token generation using SecureRandom
- Email sending with occasional failures
- Complete verification failure when email fails (no retry)

**Problems you'll observe:**
1. No retry logic - transient email failures cause permanent failures
2. No durability - process crash would lose all progress
3. No visibility - can't see what step failed or why
4. Manual error handling - try/catch for everything

## Your Task

Convert this simple flow into a durable Temporal workflow with automatic retry handling.

## Breaking Down the Problem

### Think About Activities vs Workflow

**Question:** Which operations in this code are non-deterministic (could produce different results or fail unpredictably)?

Look at the two main operations:
- `generateToken()` - Uses `SecureRandom` and `Thread.sleep()`
- `sendVerificationEmail()` - Simulates external email API with random failures

**Question:** Why separate these into two activities instead of one?

Think about what should happen if the email fails but the token was already generated. Should you:
- Generate a new token and try again? (wasteful, confusing)
- Reuse the same token and retry just the email? (correct!)

This is why we need **two separate activities** - each can be retried independently.

### Pattern Recognition from Exercise 01

If you completed Exercise 01, you learned about:
- The three-layer activity pattern (interface → implementation → business logic helper)
- Interface-based workflow design
- Activity stubs and configuration
- Retry policies and timeouts
- Worker and client setup

**Exercise 02 follows the exact same patterns**, just simpler:
- Fewer activities (2 instead of 4)
- Simpler data types (just strings)
- No database interactions

### Key Concepts to Apply

#### 1. Separating Non-Deterministic Operations

**Workflow code** must be deterministic:
- Same inputs always produce same outputs
- No `Random`, `System.currentTimeMillis()`, `UUID.randomUUID()` directly in workflow
- Use `Workflow.newRandom()`, `Workflow.currentTimeMillis()`, `Workflow.randomUUID()` instead

**Activity code** can be non-deterministic:
- Call external APIs
- Use random number generators
- Read from databases
- Send emails
- Use `Thread.sleep()`, `SecureRandom`, etc.

**Question:** Where does token generation belong - workflow or activity? Why?

#### 2. State Passing Between Activities

The workflow orchestrates the flow of data:
```
Activity 1 (generate token) → returns token string
                           ↓
Activity 2 (send email) → takes email + token as input
```

The workflow stores no state itself - it just coordinates. The token is passed as a parameter.

#### 3. Retry Policies

When you create activity stubs in your workflow, you configure retry behavior:
- How many attempts?
- How long to wait between retries?
- Exponential backoff?

**Think:** For this email verification scenario, what's a reasonable retry policy?
- Too few retries: might give up on transient failures
- Too many retries: might waste resources on permanent failures

#### 4. Type Serialization

Temporal serializes workflow inputs/outputs to JSON. Keep it simple:

**Works:**
- String, int, long, boolean, double
- Simple POJOs with public fields
- Empty constructor required

**Doesn't work:**
- Enums (without custom serialization)
- Complex nested objects
- Circular references

**Question:** What data structure should your workflow return?
- Just a boolean (success/failure)?
- An object with email, token, link, and error details?

### Implementation Path

You'll need to create several components. Think about what each one does:

**Domain Models:**
- What data do you need to pass around?
- What should the workflow return?

**Activities:**
- How do you define activity interfaces in Temporal?
- What pattern did Exercise 01 use for separating interface, implementation, and business logic?

**Workflow:**
- How do you create activity stubs?
- How do you configure retry policies?
- How do you handle exceptions?

**Worker:**
- How do you register workflows and activities?
- What task queue name should you use?

**Client/Starter:**
- How do you create a workflow stub?
- How do you execute a workflow?

**Hint:** Look at Exercise 01's structure. The patterns are identical, just with different activity names and data types.

## What to Observe When Testing

After implementing your Temporal version:

1. **Automatic Retries:**
   - Run your worker and client
   - Watch the console when email fails (10% of the time)
   - See Temporal automatically retry just the email step
   - Token is NOT regenerated during retries

2. **Temporal UI:**
   - Open http://localhost:8233
   - Find your workflow execution
   - Click into it - see both activities in the timeline
   - Look at Event History - see retry attempts
   - Notice how the token is passed from activity 1 to activity 2

3. **Durability:**
   - Start a workflow execution
   - Kill the worker process (Ctrl+C) mid-execution
   - Restart the worker
   - Watch the workflow resume and complete
   - This is durable execution in action!

## Success Criteria

- Both activities execute sequentially
- Workflows complete successfully (check Temporal UI at http://localhost:8233)
- Automatic retries handle email failures (10% of the time)
- No manual retry logic in your code
- Can complete the exercise in **under 45 minutes** (first time)

## Mental Model Shift

**Before Temporal:**
> "If email fails, everything fails. Start over from scratch. Generate new token, try again."

**After Temporal:**
> "Token generated once. If email fails, Temporal retries just that step with the same token. If process crashes, workflow resumes from last completed activity. I can see the entire execution history in the UI."

## Common Pitfalls

1. **Forgetting empty constructors in POJOs**
   - Temporal needs them for deserialization
   - Add `public ClassName() {}` to all data classes

2. **Using non-deterministic functions in workflow code**
   - Don't use `System.currentTimeMillis()` or `new Random()` in workflow
   - Use `Workflow.currentTimeMillis()` or `Workflow.newRandom()` instead

3. **Not configuring retry policies**
   - Activities won't retry without explicit configuration in ActivityOptions
   - Set `RetryOptions` when creating activity stubs

4. **Mismatched task queue names**
   - Worker and client must use the same task queue name
   - Pick one name (e.g., "email-verification-tasks") and use it consistently

5. **Trying to run workflow before worker is started**
   - Start worker first (runs forever, listening)
   - Then run client (executes once, triggers workflow)

## Hints

**Java equivalents for Python concepts:**
- Python's `secrets.token_urlsafe()` → Java's `SecureRandom` + `Base64.getUrlEncoder()`
- Python's `time.sleep()` → Java's `Thread.sleep()` (okay in activities, not in workflow)
- Python's activity logging → Java's `Activity.getExecutionContext().getLogger()`
- Python's workflow logging → Java's `Workflow.getLogger(ClassName.class)`

**Pattern reference:**
- Look at Exercise 01 for the three-layer activity pattern
- See how Worker registers both workflows and activities
- Notice how Client creates workflow stubs with options
- Study the retry policy configuration in Exercise 01

**Maven commands:**
- Compile: `mvn compile`
- Run baseline: `mvn compile exec:java`
- After you implement the solution, you can add additional execution configurations

## Code Snippet Examples

**Activity Interface Pattern:**
```java
@ActivityInterface
public interface TokenGeneratorActivity {
    @ActivityMethod
    String generateToken(String email);
}
```

**Activity Options Pattern:**
```java
ActivityOptions options = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofMinutes(5))
    .setRetryOptions(RetryOptions.newBuilder()
        .setMaximumAttempts(3)
        .setInitialInterval(Duration.ofSeconds(1))
        .setMaximumInterval(Duration.ofSeconds(10))
        .setBackoffCoefficient(2.0)
        .build())
    .build();
```

**Creating Activity Stub:**
```java
private final TokenGeneratorActivity tokenGenerator =
    Workflow.newActivityStub(TokenGeneratorActivity.class, options);
```

These are patterns, not complete implementations. Discover the full structure by applying these concepts!

## Ready?

Start by thinking through what activities you need, then build from there. Reference Exercise 01 when you get stuck. You've got this!
