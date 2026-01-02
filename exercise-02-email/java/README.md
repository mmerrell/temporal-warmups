# Exercise 02 - Email Verification (Java)

## Scenario

You have a simple two-step email verification workflow:
1. Generate a unique verification token
2. Send an email with that token

This is a **muscle memory builder** - practice the basic Temporal workflow pattern without the complexity of databases or multiple data models. Target: 30-45 minutes.

## Run the Pre-Temporal Baseline

**Navigate to the exercise directory:**
```bash
cd exercise-02-email/java
```

**Run the baseline:**
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

### Step 3: Implement the Workflow - Write the Complete Logic

**This is the key step!** Write the COMPLETE workflow implementation now, even though the activities don't exist yet. Your IDE will show red squiggles - that's perfect! We'll use the IDE to generate what's missing.

**Create:** `src/main/java/solution/temporal/EmailVerificationWorkflowImpl.java`

**Write the complete implementation:**

```java
package solution.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import solution.domain.VerificationResult;
import java.time.Duration;

public class EmailVerificationWorkflowImpl implements EmailVerificationWorkflow {

    // Configure retry policy - think about this!
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(5))  // Max time per attempt
        .setRetryOptions(RetryOptions.newBuilder()
            .setMaximumAttempts(3)           // 10% failure rate, so 3 tries is reasonable
            .setInitialInterval(Duration.ofSeconds(1))     // Wait 1s before first retry
            .setMaximumInterval(Duration.ofSeconds(10))    // Cap at 10s
            .setBackoffCoefficient(2.0)      // Double the wait each time (1s, 2s, 4s...)
            .build())
        .build();

    // Create activity stubs - these interfaces don't exist yet!
    // Your IDE will complain with red squiggles - ignore for now
    private final TokenGeneratorActivity tokenGenerator =
        Workflow.newActivityStub(TokenGeneratorActivity.class, ACTIVITY_OPTIONS);

    private final EmailSenderActivity emailSender =
        Workflow.newActivityStub(EmailSenderActivity.class, ACTIVITY_OPTIONS);

    @Override
    public VerificationResult run(String email) {
        Workflow.getLogger(EmailVerificationWorkflowImpl.class)
            .info("Starting email verification for " + email);

        try {
            // Step 1: Generate token (activity call)
            String token = tokenGenerator.generateToken(email);

            // Step 2: Send email with that token (activity call)
            String verificationLink = emailSender.sendVerificationEmail(email, token);

            Workflow.getLogger(EmailVerificationWorkflowImpl.class)
                .info("✓ Verification complete for " + email);

            return new VerificationResult(true, email, token, verificationLink);

        } catch (Exception e) {
            Workflow.getLogger(EmailVerificationWorkflowImpl.class)
                .error("✗ Verification failed for " + email + ": " + e.getMessage());

            VerificationResult result = new VerificationResult();
            result.success = false;
            result.email = email;
            result.error = e.getMessage();
            return result;
        }
    }
}
```

**What you just did:**
- Configured retry behavior (3 attempts, exponential backoff)
- Declared you need two activities (even though they don't exist)
- Wrote the orchestration logic (call activity 1, pass result to activity 2)
- Handled success and failure cases

**The insight:** You're thinking from the workflow perspective: "I need to generate a token, then send an email." You haven't worried about HOW yet - just WHAT needs to happen. This is workflow-first thinking!

**IDE showing errors?** Good! That means you're ready for Step 4.

### Step 4: Use IDE to Generate Activity Interfaces

**Now the magic happens!** Your IDE sees the red squiggles and can auto-generate the missing interfaces.

**For `TokenGeneratorActivity`:**

**IntelliJ IDEA:**
1. Click on the red `TokenGeneratorActivity`
2. Press `Alt+Enter` (Windows/Linux) or `⌥+Enter` (Mac)
3. Select "Create interface 'TokenGeneratorActivity'"
4. Choose package: `solution.temporal`

**VS Code:**
1. Click on the red `TokenGeneratorActivity`
2. Click the lightbulb or press `Ctrl+.`
3. Select "Create interface 'TokenGeneratorActivity'"

**What the IDE generates:**
```java
package solution.temporal;

public interface TokenGeneratorActivity {
    String generateToken(String email);
}
```

**Now add Temporal annotations:**
```java
package solution.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TokenGeneratorActivity {
    @ActivityMethod
    String generateToken(String email);
}
```

**Repeat for `EmailSenderActivity`:**
- Same process
- Method signature: `String sendVerificationEmail(String email, String token)`
- Add `@ActivityInterface` and `@ActivityMethod` annotations

**The insight:** You defined what you NEED (in the workflow). The IDE figured out the method signature from how you called it. You just add Temporal annotations to make it official!

### Step 5: Use IDE to Generate Activity Implementations

**More IDE magic!** Now generate the implementation classes.

**For `TokenGeneratorActivityImpl`:**

**IntelliJ IDEA:**
1. Put your cursor on `TokenGeneratorActivity` interface
2. Press `Alt+Enter` → "Create implementation"
3. Name it: `TokenGeneratorActivityImpl`

**VS Code / Manual:**
Create `src/main/java/solution/temporal/TokenGeneratorActivityImpl.java`:

```java
package solution.temporal;

public class TokenGeneratorActivityImpl implements TokenGeneratorActivity {
    private final TokenGenerator tokenGenerator;

    public TokenGeneratorActivityImpl(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public String generateToken(String email) {
        return tokenGenerator.generateToken(email);
    }
}
```

**Repeat for `EmailSenderActivityImpl`:**
- Same pattern
- Constructor takes `EmailSender emailSender`
- Delegates to `emailSender.sendVerificationEmail(email, token)`

**The insight:** These are thin adapters. They connect Temporal (the interface) to your business logic (the helper). Constructor injection makes it testable!

### Step 6: Create Business Logic Helpers

**Finally, the actual work!** Your IDE is complaining about `TokenGenerator` and `EmailSender` not existing. Create them!

**Look at `exercise/EmailVerificationService.java`** - you already have this logic! Extract it:

**Create:** `src/main/java/solution/temporal/TokenGenerator.java`
- Copy the `generateToken()` method from the baseline
- Use `SecureRandom` and `Base64.getUrlEncoder()`
- 300ms sleep to simulate work

**Create:** `src/main/java/solution/temporal/EmailSender.java`
- Copy the `sendVerificationEmail()` method from the baseline
- 10% random failure: `if (random.nextDouble() < 0.1) throw new RuntimeException(...)`
- 500ms sleep to simulate API call
- Return the verification link

**The insight:** This is pure Java with no Temporal dependencies. You could reuse these classes anywhere. The separation is clean!

### Step 7: Create the Worker

**What are you creating?** The execution engine - the process that actually runs your workflow and activity code.

**Create:** `src/main/java/solution/temporal/WorkerApp.java`

**Structure:**
```java
package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {
    private static final String TASK_QUEUE = "email-verification-tasks";

    public static void main(String[] args) {
        // 1. Connect to Temporal server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker factory and worker on task queue
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // 3. Register workflow implementation
        worker.registerWorkflowImplementationTypes(EmailVerificationWorkflowImpl.class);

        // 4. Create business logic helpers
        TokenGenerator tokenGenerator = new TokenGenerator();
        EmailSender emailSender = new EmailSender();

        // 5. Register activity implementations (with dependency injection!)
        worker.registerActivitiesImplementations(
            new TokenGeneratorActivityImpl(tokenGenerator),
            new EmailSenderActivityImpl(emailSender)
        );

        // 6. Start the worker - runs forever
        System.out.println("Worker starting on task queue: " + TASK_QUEUE);
        factory.start();
        System.out.println("Worker started. Listening for workflows...");
    }
}
```

**The insight:** Workers are stateless executors. You can run multiple workers on the same task queue and Temporal load-balances across them!

### Step 8: Create the Starter (Client)

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


### Step 9: Update Maven Configuration (Already Done!)

**Good news:** The `pom.xml` already has execution profiles configured! You can run:
- `mvn compile exec:java` → baseline
- `mvn compile exec:java@worker` → your worker
- `mvn compile exec:java@workflow` → your starter

**Note:** All Maven commands assume you're in the `exercise-02-email/java` directory:
```bash
cd exercise-02-email/java
mvn compile exec:java@worker
```

### Step 10: Final Check - Did You Get Everything?

**Review your file structure:**
```
src/main/java/
├── solution/
│   ├── domain/
│   │   └── VerificationResult.java          ✓ Step 1
│   └── temporal/
│       ├── EmailVerificationWorkflow.java    ✓ Step 2
│       ├── EmailVerificationWorkflowImpl.java ✓ Step 3
│       ├── TokenGeneratorActivity.java       ✓ Step 4
│       ├── EmailSenderActivity.java          ✓ Step 4
│       ├── TokenGeneratorActivityImpl.java   ✓ Step 5
│       ├── EmailSenderActivityImpl.java      ✓ Step 5
│       ├── TokenGenerator.java               ✓ Step 6
│       ├── EmailSender.java                  ✓ Step 6
│       ├── WorkerApp.java                    ✓ Step 7
│       └── Starter.java                      ✓ Step 8
```

**Compile check:**
```bash
cd exercise-02-email/java
mvn compile
```

No errors? You're ready to run!


## Running Your Solution

**Important:** All commands assume you're in the exercise directory:
```bash
cd exercise-02-email/java
```

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

Open in browser:
```
http://localhost:8233
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

## Advanced

How do we run more workers in parallel instead of serially?

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
