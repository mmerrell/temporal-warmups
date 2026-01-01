# Exercise 02 - Email Verification (Java)

## Scenario

You have a simple two-step email verification workflow:
1. Generate a unique verification token
2. Send an email with that token

This is a **muscle memory builder** - practice the basic Temporal workflow pattern without the complexity of databases or multiple data models. Target: 30-45 minutes.

## Run the Pre-Temporal Baseline

```bash
mvn compile exec:java
```

Watch what happens when the email service fails (10% random failure rate). The entire verification fails - no retries, no recovery.

**Problems you'll observe:**
1. No retry logic - transient email failures cause permanent failures
2. No durability - process crash would lose all progress
3. No visibility - can't see what step failed or why
4. Manual error handling - try/catch for everything

## Your Task

Convert this into a durable Temporal workflow. We'll build it **backwards** - starting with the workflow (what we want to happen), then the worker and starter (how we run it), then implementing the activities they need.

Why backwards? Because understanding the goal first makes the implementation choices clearer.

## Implementation Guide - Building From the Goal

### Step 1: Create the Domain Model

**What are you creating?** A result object that your workflow returns.

**Think about it:** What information does the caller need to know?
- Did verification succeed?
- What email was verified?
- What was the token (for testing/logging)?
- If it failed, what went wrong?

**Create:** `src/main/java/solution/domain/VerificationResult.java`

**Structure it with:**
```java
public class VerificationResult {
    public boolean success;
    // What other fields? Think about what the workflow produces...

    // Why do you need an empty constructor?
    // Hint: Temporal deserializes this from JSON

    // Add a helpful toString() for debugging
}
```

**The insight:** This is your contract between the workflow and its caller. Keep it simple - just data, no logic. Public fields are fine for DTOs in Temporal (it's not breaking encapsulation since there's no invariant to protect).

### Step 2: Create the Workflow Interface

**What are you creating?** The public API of your workflow - what does it do, what does it need, what does it return?

**Think about it:**
- What's the workflow's job? (Verify an email address)
- What input does it need? (Just an email string)
- What should it return? (Your VerificationResult from Step 1)

**Create:** `src/main/java/solution/temporal/EmailVerificationWorkflow.java`

**Structure it with:**
```java
@WorkflowInterface
public interface EmailVerificationWorkflow {
    @WorkflowMethod
    // What's the method signature?
    // Takes: an email (String)
    // Returns: VerificationResult
}
```

**The insight:** Temporal workflows are defined by interfaces, not classes. Why? Because Temporal creates dynamic proxies to route calls through its orchestration engine. The interface is your contract - the implementation is where the magic happens.

### Step 3: Implement the Workflow

**What are you creating?** The orchestration brain - what steps happen, in what order, with what data.

**Think about the flow:**
1. Generate a token (activity)
2. Send email with that token (activity)
3. Return success or failure

**Create:** `src/main/java/solution/temporal/EmailVerificationWorkflowImpl.java`

**Start with the skeleton:**
```java
public class EmailVerificationWorkflowImpl implements EmailVerificationWorkflow {

    // First, configure how activities should behave
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(?))  // How long can one attempt take?
        .setRetryOptions(RetryOptions.newBuilder()
            .setMaximumAttempts(?)     // How many times to retry?
            .setInitialInterval(?)      // Wait how long before first retry?
            .setBackoffCoefficient(?)   // Multiply wait time by what?
            .build())
        .build();

    // Create activity stubs (you'll define these interfaces in Step 6)
    // What two activities do you need? Token generation and email sending...

    @Override
    public VerificationResult run(String email) {
        // Step 1: Call token generation activity, get token
        // Step 2: Call email sending activity with email + token, get link
        // Step 3: Return success result
        // Handle exceptions and return failure result
    }
}
```

**Questions to guide you:**
- Why are activity stubs created as fields, not local variables?
- What should `setMaximumAttempts(?)` be? Think about the 10% failure rate
- Why exponential backoff? (Hint: gives failing services time to recover)
- How do you call an activity? (Look at how you create the stub - it's type-safe!)

**The insight:** The workflow is pure coordination - it doesn't DO the work, it ORCHESTRATES it. Notice you're not calling `new TokenGenerator()` - you're calling through activity stubs. Temporal intercepts these calls and makes them durable. If the worker crashes between generating the token and sending the email, Temporal will resume and skip straight to the email step!

### Step 4: Create the Worker

**What are you creating?** The execution engine - the process that actually runs your workflow and activity code.

**Think about it:** A workflow is just a plan. Someone has to execute it! That's the worker.

**Create:** `src/main/java/solution/temporal/WorkerApp.java`

**Structure:**
```java
public class WorkerApp {
    private static final String TASK_QUEUE = "email-verification-tasks";

    public static void main(String[] args) {
        // 1. Connect to Temporal server
        // Create WorkflowServiceStubs (connects to localhost:7233)
        // Create WorkflowClient from service

        // 2. Create a worker on your task queue
        // WorkerFactory, then Worker

        // 3. Tell the worker what workflows it can execute
        // worker.registerWorkflowImplementationTypes(?)

        // 4. Tell the worker what activities it can execute
        // (Skip for now - we'll add this in Step 10)

        // 5. Start the worker - it runs forever, polling for tasks
        // factory.start()
    }
}
```

**Questions to ponder:**
- Why a task queue? (Hint: it's how workflows find workers)
- Why does the worker run forever? (It's polling for work constantly)
- What if you have multiple workers on the same queue? (Temporal load-balances!)

**The insight:** Workers are stateless executors. You can start 10 workers on the same task queue and Temporal will distribute work among them. Scale horizontally by adding more workers - no code changes needed!

### Step 5: Create the Starter (Client)

**What are you creating?** The trigger - what starts workflow executions.

**Think about it:** You've defined the workflow, you've created the worker to execute it. But how do you actually RUN a workflow? That's what the starter does.

**Create:** `src/main/java/solution/temporal/Starter.java`

**Build it to:**
```java
public class Starter {
    private static final String TASK_QUEUE = // Same as worker!

    public static void main(String[] args) {
        // 1. Connect to Temporal server (same as worker)

        // 2. Define test emails
        String[] emails = {"alice@example.com", "bob@example.com", "charlie@example.com"};

        // 3. For each email:
        for (String email : emails) {
            // Create a unique workflow ID (use UUID)

            // Create a workflow stub (typed to your interface!)
            // You'll need WorkflowOptions with:
            //   - setTaskQueue() - must match worker
            //   - setWorkflowId() - unique ID you just created

            // Execute the workflow by calling run(email)
            // This blocks until the workflow completes!

            // Print the result
        }

        System.exit(0);  // Exit cleanly when done
    }
}
```

**Here's the magic moment:** When you call `workflow.run(email)`, you're not calling your implementation directly! You're calling through a Temporal-created proxy that:
1. Serializes the input
2. Sends it to the Temporal server
3. Returns an ID
4. Polls for completion
5. Deserializes the result

**The insight:** The client and worker can be in completely different processes, even different languages! The client just speaks to Temporal, and Temporal coordinates with workers. This is distributed computing made simple.

### Step 6: Create Activity Interfaces

**What are you creating?** Contracts for your two activities - token generation and email sending.

**Think about it:** Your workflow needs to call these activities. What should their signatures be?

**Create two interfaces:**

`src/main/java/solution/temporal/TokenGeneratorActivity.java`:
```java
@ActivityInterface
public interface TokenGeneratorActivity {
    @ActivityMethod
    // What does token generation need? (email)
    // What does it return? (token string)
}
```

`src/main/java/solution/temporal/EmailSenderActivity.java`:
```java
@ActivityInterface
public interface EmailSenderActivity {
    @ActivityMethod
    // What does email sending need? (email + token)
    // What does it return? (verification link string)
}
```

**Why interfaces again?** Same reason as workflows - Temporal creates proxies. When your workflow calls `tokenGenerator.generateToken(email)`, Temporal intercepts this, records it in history, schedules it on a worker, handles retries, etc. The interface is the contract, the implementation is the work.

**The insight:** Keep activity interfaces simple. Primitives and simple types only. Complex objects? Create POJOs (like VerificationResult). Temporal serializes everything to JSON, so keep it serializable!

### Step 7: Create Activity Implementations

**What are you creating?** The glue between Temporal and your business logic.

**Think about it:** Why not just put the business logic directly in these implementations? Because separation of concerns! Your business logic shouldn't know about Temporal. These implementations are thin adapters.

**Create two implementations:**

`src/main/java/solution/temporal/TokenGeneratorActivityImpl.java`:
```java
public class TokenGeneratorActivityImpl implements TokenGeneratorActivity {
    private final TokenGenerator tokenGenerator;

    public TokenGeneratorActivityImpl(TokenGenerator tokenGenerator) {
        // Constructor injection - we'll pass in the helper
    }

    @Override
    public String generateToken(String email) {
        // Just delegate to the helper
        // return tokenGenerator.generateToken(email);
    }
}
```

`src/main/java/solution/temporal/EmailSenderActivityImpl.java`:
```java
public class EmailSenderActivityImpl implements EmailSenderActivity {
    private final EmailSender emailSender;

    // Same pattern - constructor injection, delegation
}
```

**Why this three-layer pattern?** (Interface → Implementation → Helper)
1. **Interface:** Temporal's contract
2. **Implementation:** Temporal-aware adapter (can use Temporal utilities like Activity.getExecutionContext())
3. **Helper:** Pure business logic, no Temporal dependencies, easy to unit test

**The insight:** You can test `TokenGenerator` without any Temporal infrastructure. You can swap implementations. You keep concerns separated. This is good software engineering!

### Step 8: Create Business Logic Helpers

**What are you creating?** The actual work - token generation and email sending.

**This is where you can finally use non-deterministic operations!**

**Create:** `src/main/java/solution/temporal/TokenGenerator.java`

Look at the baseline `EmailVerificationService.java` - you already have the `generateToken()` logic there! Copy it into a new class:
- Generate 32 random bytes using `SecureRandom`
- Encode to URL-safe Base64 string (Java's equivalent of Python's `secrets.token_urlsafe`)
- Add console logging
- Sleep 300ms to simulate work

**Create:** `src/main/java/solution/temporal/EmailSender.java`

Again, steal from the baseline's `sendVerificationEmail()`:
- Sleep 500ms to simulate API call
- Random 10% failure rate: `if (random.nextDouble() < 0.1) throw RuntimeException`
- Build verification link from token
- Return the link

**Questions to explore:**
- Why can you use `Random` here but not in the workflow?
- What happens when you throw that exception? (Temporal catches it and retries!)
- Why separate these into their own classes instead of putting logic in activity impls?

**The insight:** This is pure Java - no Temporal! You could unit test these classes with JUnit without any Temporal test infrastructure. You could reuse `TokenGenerator` in a non-Temporal project. Clean separation = flexibility.

### Step 9: Update Maven Configuration

**What are you doing?** Making it easy to run worker vs starter with different Maven commands.

**Edit `pom.xml`:** Find the `exec-maven-plugin` section (around line 59). Inside the `<plugin>` block, add execution profiles:

```xml
<executions>
    <execution>
        <id>worker</id>
        <configuration>
            <mainClass>solution.temporal.WorkerApp</mainClass>
        </configuration>
    </execution>
    <execution>
        <id>workflow</id>
        <configuration>
            <mainClass>solution.temporal.Starter</mainClass>
        </configuration>
    </execution>
</executions>
```

Now you can run:
- `mvn compile exec:java` → baseline
- `mvn compile exec:java@worker` → your worker
- `mvn compile exec:java@workflow` → your starter

### Step 10: Wire It All Together in WorkerApp

**Go back to WorkerApp.java and complete Step 4** (remember you left a TODO there):

```java
// 3. Create business logic instances (dependency injection!)
TokenGenerator tokenGenerator = new TokenGenerator();
EmailSender emailSender = new EmailSender();

// 4. Register workflow implementation
worker.registerWorkflowImplementationTypes(EmailVerificationWorkflowImpl.class);

// 5. Register activity implementations
//    Wrap helpers in activity implementations, register them
worker.registerActivitiesImplementations(
    new TokenGeneratorActivityImpl(tokenGenerator),
    new EmailSenderActivityImpl(emailSender)
);
```

**What just happened?** You created instances of your helpers, wrapped them in activity implementations, and told the worker "these are the activities you can execute." When a workflow calls an activity, Temporal routes it to the registered implementation.

**The final insight:** You've built a distributed, durable, retryable system and the business logic is just... normal Java. That's the power of Temporal - you write straightforward code, Temporal handles the hard parts.

## Running Your Solution

### Terminal 1 - Start the Worker

```bash
mvn compile exec:java@worker
```

You should see:
```
Worker starting on task queue: email-verification-tasks
Worker started. Listening for workflows...
```

Keep this running!

### Terminal 2 - Run the Starter

```bash
mvn compile exec:java@workflow
```

Watch the magic happen:
- Token generation for each email
- Email sending (with occasional retries when it fails)
- Final results printed

### Terminal 3 - Temporal UI

```bash
open http://localhost:8233
```

In the UI you'll see:
- All 3 workflow executions
- Click into one → see both activities in timeline
- View Event History → see retry attempts
- Notice token passed from activity 1 to activity 2

## What to Observe

### 1. Automatic Retries

When email sending fails (10% of the time), watch the worker console:
```
Sending verification email to bob@example.com...
Email service temporarily unavailable
Sending verification email to bob@example.com...  (retry!)
✓ Verification email sent to bob@example.com
```

Notice: Token is NOT regenerated - same token reused across retries!

### 2. Durable Execution

Try this:
1. Start worker
2. Start starter (triggers workflows)
3. **Kill worker** (Ctrl+C) mid-execution
4. Restart worker
5. Watch workflows resume exactly where they left off!

This is durable execution - your workflows survive process crashes.

### 3. Temporal UI Insights

In http://localhost:8233, find a workflow that had retries:
- Event History shows: `ActivityTaskScheduled` → `ActivityTaskFailed` → `ActivityTaskScheduled` (retry!) → `ActivityTaskCompleted`
- Input/Output tab shows the token passed between activities
- Timeline shows visual execution flow

## Success Criteria

✅ Worker starts without errors
✅ All 3 workflows complete successfully
✅ Automatic retries visible when email fails
✅ No manual retry logic in your code
✅ Can complete in **30-45 minutes**

## Mental Model Shift

**Before Temporal:**
```
Email fails → Entire process fails → Start over → Generate new token → Try again
```

**After Temporal:**
```
Email fails → Temporal retries just that step → Same token → Eventually succeeds
Process crashes → Temporal resumes from last checkpoint → No work lost
```

You're not writing retry logic. You're declaring **what should happen**, and Temporal makes it durable and resilient.

## Common Issues

**"Worker won't start"**
- Make sure Temporal server is running: `temporal server start-dev`
- Check Java version: `java -version` (should be 11+)

**"Workflow not executing"**
- Is worker running in Terminal 1?
- Check task queue names match in Worker and Starter

**"Can't see workflows in UI"**
- Open http://localhost:8233 (not :8233/)
- Click "Workflows" in left sidebar
- Filter by status if needed

**"Activity not retrying"**
- Check ActivityOptions includes RetryOptions
- Verify maximumAttempts is > 1

## What You've Learned

By building this backwards, you now understand:

1. **Workflow = Orchestration**: Calls activities, passes data, handles results
2. **Activities = Work**: Where non-deterministic operations happen
3. **Worker = Executor**: Runs workflows and activities
4. **Starter = Trigger**: Initiates workflow executions
5. **Temporal = Reliability**: Handles retries, durability, visibility automatically

You've practiced the exact pattern you'll use for every Temporal application. Next time, you'll build it even faster!
