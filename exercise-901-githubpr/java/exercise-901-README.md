# Exercise 901 - GitHub PR AI Review System ⭐⭐⭐ EXPERT LEVEL

## Scenario

You're building a **GitHub Pull Request AI Review Agent Orchestrator** that automatically reviews PRs using three specialized AI agents:

1. **Code Quality Agent** - Evaluates naming, function size, responsibilities, boundaries, and error handling
2. **Test Quality Agent** - Assesses test coverage and suggests high-value tests
3. **Security Agent** - Identifies secrets, auth bypasses, and injection vulnerabilities

This is a realistic use case for Temporal customers building **multi-agent AI orchestration** systems. Each agent makes expensive LLM API calls (OpenAI GPT-4) and can fail due to rate limits, timeouts, or network issues.

**The current implementation is intentionally brittle**. It's a synchronous Spring Boot REST API that blocks HTTP threads for 6-9+ seconds per request, has no retries, no durability, and no observability. This is the "BEFORE" state that you'll transform using Temporal.

### Why This Exercise is Expert-Level ⭐⭐⭐

This exercise introduces **two new concepts** not covered in previous exercises:

1. **Temporal Testing with TestWorkflowEnvironment** - Learn how to write integration tests for workflows using Temporal's testing framework
2. **Spring Boot Integration Patterns** - Understand different approaches for combining Temporal with Spring Boot applications
3. Handing non-deterministic timeouts;

```java
// To handle time-based logic or track a "start time" inside your Workflow, use the SDK-provided alternative:
// Use this instead of System.currentTimeMillis()
long startMs = Workflow.currentTimeMillis();
```

You'll also apply advanced patterns:
- Multi-agent orchestration with independent retry
- LLM API retry policies (aggressive retries for rate limits and timeouts)
- Deterministic aggregation logic in workflows
- Activity independence for cost efficiency

### Problems with the Non-Temporal Approach

The pre-temporal baseline demonstrates these critical issues:

1. **No Retries** - If any agent's OpenAI API call times out or returns a 500 error, the entire request fails
2. **No Durability** - If the server crashes mid-review, all progress is lost and must restart from scratch
3. **No Observability** - Cannot see intermediate results or debug agent failures
4. **Unbounded Latency** - Blocks HTTP thread for 6-9+ seconds (3 agents × 2-3 seconds each)
5. **Cannot Resume After Failure** - If Security agent fails, must re-run ALL agents (wasting time and $$$ on LLM API costs)
6. **Unsafe for Scale** - Thread-per-request model exhausts thread pool under load

**Temporal solves all of these problems** through durable workflows, automatic retries, and activity-based execution.

---

## Run the Pre-Temporal Baseline

### Prerequisites

Before starting, ensure you have:

1. **Temporal Server Running:**
   ```bash
   temporal server start-dev
   ```
   Keep this running in a separate terminal. The Temporal UI will be available at http://localhost:8233

2. **OpenAI API Key** (or use DUMMY_MODE):
   - **Option A:** Get a real OpenAI API key from https://platform.openai.com
     ```bash
     export OPENAI_API_KEY=sk-your-key-here
     ```
   - **Option B:** Use DUMMY_MODE for development (no API costs, canned responses):
     ```bash
     export DUMMY_MODE=true
     ```

3. **Java 11+ and Maven:**
   ```bash
   java -version   # Should be 11 or higher
   mvn --version
   ```

### Start the Spring Boot App

Navigate to the exercise directory and run:

```bash
cd exercise-901-githubpr/java
mvn spring-boot:run
```

You should see output like:

```
============================================================
GitHub PR AI Review System - Pre-Temporal Baseline
============================================================

⚠️  DUMMY_MODE is enabled - using canned responses
   (Set DUMMY_MODE=false to use real OpenAI API)

Starting Spring Boot server on http://localhost:8080
POST /review to analyze pull requests
GET /health for health check
============================================================
```

### Test with curl

In a new terminal, test the /review endpoint:

```bash
curl -X POST http://localhost:8080/review \
  -H "Content-Type: application/json" \
  -d '{
    "prTitle": "Add user authentication",
    "prDescription": "Implements JWT-based authentication with password hashing",
    "diff": "+function authenticateUser(username, password) {\n+  // Hash password and check against database\n+  const hashedPassword = hashPassword(password);\n+  return db.query(\"SELECT * FROM users WHERE username=? AND password=?\", [username, hashedPassword]);\n+}",
    "testSummary": {
      "passed": true,
      "totalTests": 5,
      "failedTests": 0,
      "durationMs": 120
    }
  }'
```

Expected response structure:

```json
{
  "overallRecommendation": "APPROVE",
  "agents": [
    {
      "agentName": "Code Quality",
      "riskLevel": "LOW",
      "recommendation": "APPROVE",
      "findings": ["..."]
    },
    {
      "agentName": "Test Quality",
      "riskLevel": "LOW",
      "recommendation": "APPROVE",
      "findings": ["..."]
    },
    {
      "agentName": "Security",
      "riskLevel": "LOW",
      "recommendation": "APPROVE",
      "findings": ["..."]
    }
  ],
  "metadata": {
    "generatedAt": "2026-01-09T...",
    "tookMs": 6234,
    "model": "gpt-4o-mini"
  }
}
```

### Problems to Observe

Watch the server logs and observe these issues:

1. **HTTP Request Blocks for 6-9+ Seconds**
   - The curl request hangs while agents run sequentially
   - Under load, this exhausts the HTTP thread pool

2. **If Any Agent Fails → Entire Request Fails**
   - Try disconnecting your internet mid-request
   - The entire review fails with HTTP 500, even if 2 agents succeeded

3. **No Retry Logic**
   - OpenAI rate limits (429 errors) cause immediate failure
   - Transient network issues don't recover automatically

4. **No Durability**
   - Kill the Spring Boot process (Ctrl+C) mid-review
   - When you restart, there's no way to resume - must start over

5. **No Visibility**
   - You can't see intermediate agent results
   - Can't debug why an agent failed without looking at logs

6. **Cannot Scale**
   - Try running 100 concurrent requests
   - The server will struggle to handle the load

**This is why we need Temporal!**

---

## Your Task

### Goal

Convert the brittle Spring Boot orchestrator into a **durable Temporal workflow** that solves all the problems above.

### Key Requirements

Your Temporal solution must include:

1. **3 Separate Activities** (one per agent) with independent retry policies
2. **Workflow Orchestration** that coordinates agent execution and aggregates results
3. **Deterministic Aggregation Logic** in the workflow (BLOCK if any agent blocks)
4. **Worker and Starter Classes** using the traditional pattern (separate from Spring Boot app)
5. **Integration Test** using `TestWorkflowEnvironment` to verify aggregation logic

### What to Reuse

You can (and should!) reuse these classes from the `exercise/` package:

- **Domain Models (POJOs):** Copy `ReviewRequest`, `ReviewResponse`, `AgentResult`, `TestSummary`, `Metadata` to `solution.domain/`
- **Business Logic:** Copy all agent classes (`CodeQualityAgent`, `TestQualityAgent`, `SecurityAgent`) to `solution.temporal.agents/`
- **LLM Abstraction:** Copy `LlmClient`, `OpenAiLlmClient`, `Message`, `LlmOptions` to `solution.temporal.llm/`

These are **pure business logic** with no Spring Boot or temporal-specific code. They work perfectly in both contexts.

### What to Create (in `solution/` Package)

You need to create these files:

1. **Activity Interfaces & Implementations (6 files):**
   - `CodeQualityActivity.java` (interface)
   - `CodeQualityActivityImpl.java` (implementation)
   - `TestQualityActivity.java` (interface)
   - `TestQualityActivityImpl.java` (implementation)
   - `SecurityActivity.java` (interface)
   - `SecurityActivityImpl.java` (implementation)

2. **Workflow Interface & Implementation (2 files):**
   - `ReviewWorkflow.java` (interface with `@WorkflowInterface`)
   - `ReviewWorkflowImpl.java` (implementation)

3. **Worker & Starter (2 files):**
   - `WorkerApp.java` (registers workflow and activities, runs forever)
   - `Starter.java` (creates workflow stub, executes workflow, prints results)

4. **Integration Test (1 file):**
   - `src/test/java/solution/temporal/ReviewWorkflowTest.java`

**Total:** ~11 new files to create

---

## NEW CONCEPT: Temporal Testing with TestWorkflowEnvironment

### Why This is Expert-Level

Testing Temporal workflows requires understanding **TestWorkflowEnvironment**, an in-memory Temporal server that lets you test workflows without running a real Temporal server or making actual API calls.

This is a **critical skill** for production Temporal applications:
- Fast, deterministic tests (no network dependencies)
- Verify workflow logic (aggregation, routing, error handling)
- Mock activities to simulate different scenarios
- Catch bugs before deploying to production

### TestWorkflowEnvironment Basics

**What it is:**
- In-memory Temporal server for testing
- No need for `temporal server start-dev`
- Supports mocking activities with Mockito
- Fast execution (no sleep, instant time skips)

**When to use it:**
- Integration tests that verify workflow behavior
- Testing aggregation logic (e.g., "if any agent blocks → overall blocks")
- Testing error handling and retry policies
- Testing workflow state transitions

### How to Write an Integration Test

Here's the pattern you'll use (NOT a complete solution, just the structure):

```java
package solution.temporal;

import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReviewWorkflowTest {

    // Step 1: Set up TestWorkflowExtension
    @RegisterExtension
    public static final TestWorkflowExtension testWorkflow =
        TestWorkflowExtension.newBuilder()
            .setWorkflowTypes(YourWorkflowImpl.class)  // Register your workflow
            .setDoNotStart(true)  // We'll start it manually after registering activities
            .build();

    @Test
    public void testAggregation_BlockIfAnyAgentBlocks() {
        // Step 2: Create mock activities
        CodeQualityActivity codeActivity = mock(CodeQualityActivity.class);
        TestQualityActivity testActivity = mock(TestQualityActivity.class);
        SecurityActivity securityActivity = mock(SecurityActivity.class);

        // Step 3: Register mocked activities with the test worker
        Worker worker = testWorkflow.getWorker();
        worker.registerActivitiesImplementations(codeActivity, testActivity, securityActivity);

        // Step 4: Configure mock responses
        AgentResult codeResult = new AgentResult();
        codeResult.agentName = "Code Quality";
        codeResult.recommendation = "APPROVE";  // Code Quality approves
        codeResult.riskLevel = "LOW";
        codeResult.findings = List.of("Good code structure");

        AgentResult testResult = new AgentResult();
        testResult.agentName = "Test Quality";
        testResult.recommendation = "BLOCK";  // Test Quality BLOCKS!
        testResult.riskLevel = "HIGH";
        testResult.findings = List.of("No tests provided");

        AgentResult securityResult = new AgentResult();
        securityResult.agentName = "Security";
        securityResult.recommendation = "APPROVE";  // Security approves
        securityResult.riskLevel = "LOW";
        securityResult.findings = List.of("No security issues");

        // Step 5: Wire up mocks to return these results
        when(codeActivity.analyzeCodeQuality(anyString(), anyString(), anyString()))
            .thenReturn(codeResult);
        when(testActivity.analyzeTestQuality(anyString(), anyString(), anyString(), any()))
            .thenReturn(testResult);
        when(securityActivity.analyzeSecurity(anyString(), anyString(), anyString()))
            .thenReturn(securityResult);

        // Step 6: Start the test environment
        testWorkflow.getTestEnvironment().start();

        // Step 7: Execute the workflow
        YourWorkflow workflow = testWorkflow.getWorkflowClient()
            .newWorkflowStub(YourWorkflow.class, testWorkflow.getDefaultWorkflowOptions());

        ReviewRequest request = new ReviewRequest();
        request.prTitle = "Test PR";
        request.prDescription = "Test description";
        request.diff = "... diff ...";

        ReviewResponse response = workflow.yourWorkflowMethod(request);

        // Step 8: Assert the results
        assertEquals("BLOCK", response.overallRecommendation,
            "If any agent blocks, overall should be BLOCK");
        assertEquals(3, response.agents.size());

        // Step 9: Verify all activities were called
        verify(codeActivity, times(1)).analyzeCodeQuality(anyString(), anyString(), anyString());
        verify(testActivity, times(1)).analyzeTestQuality(anyString(), anyString(), anyString(), any());
        verify(securityActivity, times(1)).analyzeSecurity(anyString(), anyString(), anyString());
    }
}
```

### What to Test

Write integration tests for these scenarios:

1. **Aggregation: BLOCK if any agent blocks**
   - Code Quality: APPROVE
   - Test Quality: BLOCK
   - Security: APPROVE
   - Expected: Overall = BLOCK

2. **Aggregation: REQUEST_CHANGES if any agent requests changes**
   - Code Quality: REQUEST_CHANGES
   - Test Quality: APPROVE
   - Security: APPROVE
   - Expected: Overall = REQUEST_CHANGES

3. **Aggregation: APPROVE if all agents approve**
   - Code Quality: APPROVE
   - Test Quality: APPROVE
   - Security: APPROVE
   - Expected: Overall = APPROVE

### Key Testing Concepts

**Mocking with Mockito:**
- Use `mock(YourActivity.class)` to create activity mocks
- Use `when(...).thenReturn(...)` to configure responses
- Use `verify(...)` to assert methods were called

**TestWorkflowExtension:**
- Manages test lifecycle (setup, teardown)
- Provides access to test environment and worker
- Use `@RegisterExtension` (JUnit 5) instead of `@Rule` (JUnit 4)

**Important:**
- Always call `testWorkflow.getTestEnvironment().start()` before executing workflows
- Forgot this? Your test will hang or fail with cryptic errors
- Register activities BEFORE starting the test environment

---

## Spring Boot Integration Patterns

### Two Approaches for Temporal + Spring Boot

When building Temporal applications with Spring Boot, you have two architectural choices:

#### 1. **Traditional Pattern** (This Exercise)

Separate components:
- **Spring Boot app** runs REST API (if needed)
- **Temporal Worker** runs in separate process (`WorkerApp.java`)
- **Temporal Client** runs in separate process (`Starter.java`)

**Directory structure:**
```
exercise.Application        # Spring Boot REST API
solution.temporal.WorkerApp # Temporal Worker (separate JVM)
solution.temporal.Starter   # Temporal Client (separate JVM)
```

**How to run:**
```bash
# Terminal 1: Spring Boot REST API (optional, not needed for pure Temporal)
mvn spring-boot:run

# Terminal 2: Temporal Worker
mvn compile exec:java@worker

# Terminal 3: Temporal Client (Starter)
mvn compile exec:java@workflow
```

**Pros:**
- Clear separation of concerns
- Worker can scale independently from API
- Easier to understand for learning
- Matches exercises 01-06 pattern

**Cons:**
- More processes to manage
- No shared Spring context between Worker and API

#### 2. **Integrated Pattern** (Advanced, Not This Exercise)

Single Spring Boot application contains everything:
- REST API endpoints
- Temporal workers (embedded)
- Workflow/activity beans managed by Spring

**How it works:**
- REST endpoint triggers Temporal workflow
- Returns immediately (async)
- Workers run in same Spring Boot process
- Spring manages dependency injection for activities

**When to use:**
- Production applications that need tight Spring integration
- When you want Spring to manage Temporal worker lifecycle
- When activities need Spring beans (repositories, services)

**Official docs:** https://docs.temporal.io/develop/java/spring-boot-integration

### Why This Exercise Uses Traditional Pattern

For **learning purposes**, the traditional pattern is superior:

1. **Clearer mental model** - Separate processes for separate concerns
2. **Matches previous exercises** - Consistency with exercises 01-06
3. **Easier debugging** - Can see Worker and Client logs separately
4. **Simpler testing** - TestWorkflowEnvironment doesn't need Spring context

In production, you might choose the integrated pattern for operational simplicity and Spring dependency injection. But for learning Temporal fundamentals, the traditional pattern is better.

### For This Exercise: Follow Traditional Pattern

Your task is to create:
- `WorkerApp.java` - Standalone Java class with `main()` method
- `Starter.java` - Standalone Java class with `main()` method
- Both connect to Temporal Server at `localhost:7233`
- Neither depends on Spring Boot

**Hint:** Look at exercise-06's `WorkerApp.java` and `Starter.java` for reference.

---

## Multi-Agent AI Orchestration

### Why 3 Separate Activities Instead of 1 Combined Activity?

**Bad Approach (Don't Do This):**
```java
@ActivityInterface
public interface ReviewActivity {
    ReviewResponse reviewPR(ReviewRequest request);  // Calls all 3 agents inside one activity
}
```

**Good Approach (Do This):**
```java
@ActivityInterface
public interface CodeQualityActivity {
    AgentResult analyzeCodeQuality(String prTitle, String prDescription, String diff);
}

@ActivityInterface
public interface TestQualityActivity {
    AgentResult analyzeTestQuality(String prTitle, String prDescription, String diff, TestSummary testSummary);
}

@ActivityInterface
public interface SecurityActivity {
    AgentResult analyzeSecurity(String prTitle, String prDescription, String diff);
}
```

### Why Separate Activities?

#### 1. **Independent Retry**

If Security agent fails, Temporal can retry JUST that activity. Code Quality and Test Quality results are preserved:

```
[Workflow Execution Timeline]
1. Code Quality Activity → SUCCESS (cached result)
2. Test Quality Activity → SUCCESS (cached result)
3. Security Activity → FAILURE (OpenAI rate limit)
   ↓ [Temporal automatically retries]
4. Security Activity → SUCCESS (retry succeeded)
```

With a combined activity, all 3 agents would re-run on retry, wasting time and money.

#### 2. **Cost Efficiency**

LLM API calls are expensive:
- GPT-4: $0.03 per 1K input tokens, $0.06 per 1K output tokens
- Average PR review: ~2K tokens per agent = ~$0.12 per agent
- 3 agents = **$0.36 per PR review**

If you retry all 3 agents when only 1 fails:
- Wasted cost: $0.24 per retry
- With 5 retries: **$1.20 wasted** per PR

Separate activities = retry only what failed = **huge cost savings** at scale.

#### 3. **Audit Trail**

Temporal UI shows each activity separately:
```
Workflow: pr-review-12345
  ├─ Activity: CodeQualityActivity → COMPLETED (2.3s)
  ├─ Activity: TestQualityActivity → COMPLETED (1.8s)
  └─ Activity: SecurityActivity → FAILED (timeout) → RETRY → COMPLETED (2.1s)
```

You can see exactly which agent failed and why, with full event history.

#### 4. **Observability**

Separate activities mean separate metrics:
- Average duration per agent type
- Failure rates per agent type
- Cost attribution per agent type
- Retry counts per agent type

This data is invaluable for optimizing your system.

### Activity vs Workflow Decision Framework

**Activities (Non-Deterministic Operations):**
- LLM API calls (OpenAI, Anthropic, etc.)
- Database reads/writes
- HTTP requests to external services
- File I/O
- Anything that can fail unpredictably

**Workflow (Deterministic Logic):**
- Routing decisions based on agent results
- Aggregation logic (if any agent blocks → overall blocks)
- Data transformations
- Conditional logic based on inputs

### Question: Where Does Aggregation Logic Belong?

The aggregation logic ("if any agent blocks → overall blocks") is **deterministic**:
- Given the same 3 agent results, it always produces the same output
- No external dependencies
- Pure function: `f(results) => overallRecommendation`

**Answer: Aggregation belongs in the WORKFLOW, not activities!**

```java
// ✅ CORRECT: Aggregation in workflow
public class ReviewWorkflowImpl implements ReviewWorkflow {
    @Override
    public ReviewResponse review(ReviewRequest request) {
        AgentResult codeQuality = codeQualityActivity.analyze(...);
        AgentResult testQuality = testQualityActivity.analyze(...);
        AgentResult security = securityActivity.analyze(...);

        // Deterministic aggregation - belongs here!
        String overall = aggregate(codeQuality, testQuality, security);

        return new ReviewResponse(overall, List.of(codeQuality, testQuality, security), ...);
    }

    private String aggregate(AgentResult... results) {
        for (AgentResult result : results) {
            if ("BLOCK".equals(result.recommendation)) return "BLOCK";
        }
        for (AgentResult result : results) {
            if ("REQUEST_CHANGES".equals(result.recommendation)) return "REQUEST_CHANGES";
        }
        return "APPROVE";
    }
}
```

**Why not in activities?**
- Aggregation doesn't need to make API calls or access external state
- It's pure logic that should be part of the workflow's decision-making
- Keeping it in the workflow makes the workflow history more meaningful

---

## Implementation Path

Follow these steps to build your Temporal solution:

### Step 1: Create Package Structure

Create the following directory structure under `src/main/java/`:

```
solution/
├── domain/                 # Copy POJOs from exercise.model/
├── temporal/
│   ├── llm/                # Copy LLM classes from exercise.llm/
│   ├── agents/             # Copy agent classes from exercise.agents/
│   ├── activity/           # Activity interfaces and implementations
│   ├── ReviewWorkflow.java
│   ├── ReviewWorkflowImpl.java
│   ├── WorkerApp.java
│   └── Starter.java
```

**Hint:** Use your IDE or `mkdir -p` to create these directories.

### Step 2: Copy Reusable Code

Copy these classes from `exercise/` to `solution/`:

**Domain Models** (exercise.model → solution.domain):
- ReviewRequest.java
- ReviewResponse.java
- AgentResult.java
- TestSummary.java
- Metadata.java

**LLM Abstraction** (exercise.llm → solution.temporal.llm):
- LlmClient.java
- OpenAiLlmClient.java
- Message.java
- LlmOptions.java

**Business Logic** (exercise.agents → solution.temporal.agents):
- CodeQualityAgent.java
- TestQualityAgent.java
- SecurityAgent.java

**Important:** Update package declarations after copying!

### Step 3: Create Activity Interfaces (6 Files)

For each agent, create an interface and implementation.

**Pattern: Activity Interface**

```java
package solution.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import solution.domain.AgentResult;

@ActivityInterface
public interface CodeQualityActivity {
    @ActivityMethod
    AgentResult analyzeCodeQuality(String prTitle, String prDescription, String diff);
}
```

**Pattern: Activity Implementation**

```java
package solution.temporal.activity;

import solution.domain.AgentResult;
import solution.temporal.agents.CodeQualityAgent;

public class CodeQualityActivityImpl implements CodeQualityActivity {

    private final CodeQualityAgent agent;

    public CodeQualityActivityImpl(CodeQualityAgent agent) {
        this.agent = agent;
    }

    @Override
    public AgentResult analyzeCodeQuality(String prTitle, String prDescription, String diff) {
        // Delegate to business logic
        return agent.analyze(prTitle, prDescription, diff);
    }
}
```

**Repeat this pattern for:**
- `TestQualityActivity` + `TestQualityActivityImpl`
- `SecurityActivity` + `SecurityActivityImpl`

**Hint:** The implementation is just a thin wrapper that delegates to the agent classes.

### Step 4: Create Workflow Interface

Create `solution.temporal.ReviewWorkflow`:

```java
package solution.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import solution.domain.ReviewRequest;
import solution.domain.ReviewResponse;

@WorkflowInterface
public interface ReviewWorkflow {
    @WorkflowMethod
    ReviewResponse review(ReviewRequest request);
}
```

**Key points:**
- Single `@WorkflowMethod` that takes ReviewRequest and returns ReviewResponse
- This is the public contract for your workflow
- Look at exercise-06's `SupportTriageWorkflow` for reference

### Step 5: Create Workflow Implementation

Create `solution.temporal.ReviewWorkflowImpl`:

**Key concepts you must implement:**

1. **Activity Options with Retry Policy:**
   ```java
   private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
       .setStartToCloseTimeout(Duration.ofSeconds(60))
       .setRetryOptions(RetryOptions.newBuilder()
           .setMaximumAttempts(5)             // LLM APIs need aggressive retries
           .setInitialInterval(Duration.ofSeconds(2))  // Start with 2s delay
           .setBackoffCoefficient(2.0)        // Exponential backoff
           .build())
       .build();
   ```

2. **Create Activity Stubs:**
   ```java
   private final CodeQualityActivity codeQualityActivity =
       Workflow.newActivityStub(CodeQualityActivity.class, ACTIVITY_OPTIONS);
   private final TestQualityActivity testQualityActivity =
       Workflow.newActivityStub(TestQualityActivity.class, ACTIVITY_OPTIONS);
   private final SecurityActivity securityActivity =
       Workflow.newActivityStub(SecurityActivity.class, ACTIVITY_OPTIONS);
   ```

3. **Execute Activities in Workflow:**
   ```java
   @Override
   public ReviewResponse review(ReviewRequest request) {
       long startMs = Workflow.currentTimeMillis();  // NOT System.currentTimeMillis()!

       // Execute activities sequentially (each can retry independently)
       AgentResult codeQuality = codeQualityActivity.analyzeCodeQuality(...);
       AgentResult testQuality = testQualityActivity.analyzeTestQuality(...);
       AgentResult security = securityActivity.analyzeSecurity(...);

       // Aggregate results (deterministic!)
       String overall = aggregate(codeQuality, testQuality, security);

       // Build and return response
       return new ReviewResponse(...);
   }
   ```

4. **Implement Aggregation Logic:**
   ```java
   private String aggregate(AgentResult... results) {
       // If ANY agent returns BLOCK → overall is BLOCK
       for (AgentResult result : results) {
           if ("BLOCK".equals(result.recommendation)) {
               return "BLOCK";
           }
       }

       // Else if ANY returns REQUEST_CHANGES → overall is REQUEST_CHANGES
       for (AgentResult result : results) {
           if ("REQUEST_CHANGES".equals(result.recommendation)) {
               return "REQUEST_CHANGES";
           }
       }

       // All agents approved → overall is APPROVE
       return "APPROVE";
   }
   ```

**Critical:** Use `Workflow.currentTimeMillis()` instead of `System.currentTimeMillis()` for determinism!

### Step 6: Create WorkerApp

Create `solution.temporal.WorkerApp`:

**Pattern:**

```java
package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {
    public static void main(String[] args) {
        // 1. Connect to Temporal Server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // 2. Create worker for task queue
        String taskQueue = "github-pr-review";  // IMPORTANT: Consistent with Starter!
        Worker worker = factory.newWorker(taskQueue);

        // 3. Create business logic instances
        LlmClient llmClient = new OpenAiLlmClient();
        CodeQualityAgent codeAgent = new CodeQualityAgent(llmClient);
        TestQualityAgent testAgent = new TestQualityAgent(llmClient);
        SecurityAgent securityAgent = new SecurityAgent(llmClient);

        // 4. Register workflow implementation
        worker.registerWorkflowImplementationTypes(ReviewWorkflowImpl.class);

        // 5. Register activity implementations
        worker.registerActivitiesImplementations(
            new CodeQualityActivityImpl(codeAgent),
            new TestQualityActivityImpl(testAgent),
            new SecurityActivityImpl(securityAgent)
        );

        // 6. Start the worker
        factory.start();
        System.out.println("Worker started for task queue: " + taskQueue);
        // Worker runs forever until JVM exits
    }
}
```

**Key points:**
- Task queue name must match Starter's task queue
- Register workflow types (not instances!)
- Register activity implementations (instances with dependencies)
- Worker blocks forever (keep running in Terminal 2)

**Hint:** Look at exercise-06's `WorkerApp.java` for reference.

### Step 7: Create Starter

Create `solution.temporal.Starter`:

**Pattern:**

```java
package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import solution.domain.*;

import java.util.UUID;

public class Starter {
    public static void main(String[] args) {
        // 1. Connect to Temporal Server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create workflow stub
        String taskQueue = "github-pr-review";  // IMPORTANT: Match WorkerApp!
        String workflowId = "pr-review-" + UUID.randomUUID();

        ReviewWorkflow workflow = client.newWorkflowStub(
            ReviewWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId(workflowId)
                .build()
        );

        // 3. Create sample ReviewRequest
        ReviewRequest request = new ReviewRequest();
        request.prTitle = "Add user authentication";
        request.prDescription = "Implements JWT-based authentication";
        request.diff = "+function authenticateUser(username, password) {...}";

        TestSummary testSummary = new TestSummary();
        testSummary.passed = true;
        testSummary.totalTests = 5;
        testSummary.failedTests = 0;
        testSummary.durationMs = 120;
        request.testSummary = testSummary;

        // 4. Execute workflow (blocks until complete)
        System.out.println("Starting workflow: " + workflowId);
        ReviewResponse response = workflow.review(request);

        // 5. Print results
        System.out.println("=".repeat(60));
        System.out.println("Overall Recommendation: " + response.overallRecommendation);
        for (AgentResult agent : response.agents) {
            System.out.println("\n" + agent.agentName + ":");
            System.out.println("  Risk: " + agent.riskLevel);
            System.out.println("  Recommendation: " + agent.recommendation);
            System.out.println("  Findings:");
            for (String finding : agent.findings) {
                System.out.println("    - " + finding);
            }
        }
        System.out.println("\nMetadata:");
        System.out.println("  Generated: " + response.metadata.generatedAt);
        System.out.println("  Duration: " + response.metadata.tookMs + "ms");
        System.out.println("  Model: " + response.metadata.model);
        System.out.println("=".repeat(60));

        System.exit(0);
    }
}
```

**Key points:**
- Task queue must match WorkerApp
- Workflow ID should be unique (use UUID)
- workflow.review() is a synchronous call that blocks until workflow completes
- Print detailed results to verify behavior

**Hint:** Look at exercise-06's `Starter.java` for reference.

### Step 8: Create Integration Test

Create `src/test/java/solution/temporal/ReviewWorkflowTest.java`:

Use the pattern shown in the "Temporal Testing" section above.

**What to test:**
- Aggregation logic: BLOCK if any agent blocks
- Aggregation logic: REQUEST_CHANGES if any agent requests changes
- Aggregation logic: APPROVE if all agents approve

**Hint:** Start with one test case, get it passing, then add more.

---

## Success Criteria

Your solution is complete when all of these are true:

### ✅ Worker Starts Without Errors

```bash
mvn compile exec:java@worker
```

Expected output:
```
Worker started for task queue: github-pr-review
```

No exceptions, no errors. Worker keeps running.

### ✅ Starter Executes Workflow Successfully

```bash
mvn compile exec:java@workflow
```

Expected output:
```
Starting workflow: pr-review-...
============================================================
Overall Recommendation: APPROVE
...
============================================================
```

Prints ReviewResponse with 3 agent results.

### ✅ Temporal UI Shows Workflow Execution

Navigate to http://localhost:8233

- Find your workflow by ID (printed by Starter)
- See workflow status: "Completed"
- Open workflow details
- See 3 "ActivityTaskScheduled" events
- See 3 "ActivityTaskCompleted" events
- Click on each activity to see input/output

### ✅ Integration Test Passes

```bash
mvn test
```

Expected output:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### ✅ Aggregation Logic Works Correctly

Manually verify by modifying Starter to test different scenarios:

**Scenario 1:** All agents approve
- Expected: Overall = APPROVE ✓

**Scenario 2:** One agent blocks (mock Security to return BLOCK)
- Expected: Overall = BLOCK ✓

**Scenario 3:** One agent requests changes (mock Test Quality to return REQUEST_CHANGES)
- Expected: Overall = REQUEST_CHANGES ✓

### ✅ Each Activity Has Independent Retry Configuration

Check workflow implementation:
- All activity stubs created with `ActivityOptions`
- RetryOptions configured with maxAttempts, initialInterval, backoffCoefficient
- Each activity can retry independently

### ✅ Workflow Uses Temporal APIs for Non-Determinism

Check workflow implementation:
- Uses `Workflow.currentTimeMillis()` instead of `System.currentTimeMillis()`
- Uses `Workflow.newRandom()` if generating random values
- No direct external dependencies in workflow code

### ✅ All POJOs Have No-Arg Constructors

Check domain models:
- Each POJO has `public ClassName() {}` constructor
- Required for JSON deserialization by Temporal

---

## Common Pitfalls

Here are mistakes learners commonly make, with solutions:

| **Mistake** | **Symptom** | **Solution** |
|-------------|-------------|-------------|
| **Forgot no-arg constructor in POJOs** | `com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Cannot construct instance` | Add `public ClassName() {}` to all domain models (ReviewRequest, ReviewResponse, AgentResult, etc.) |
| **Used `System.currentTimeMillis()` in workflow** | Workflow non-determinism warning in logs | Use `Workflow.currentTimeMillis()` instead |
| **Put business logic in workflow code** | Workflow code is bloated and hard to test | Keep workflow lean - delegate to activities. Business logic belongs in agent classes, not workflow. |
| **Didn't configure retry for activities** | Single API failure → entire workflow fails permanently | Set RetryOptions on ActivityOptions with maxAttempts, initialInterval, backoffCoefficient |
| **Mismatched task queue names** | Worker never picks up work; workflow hangs forever | Use identical string in WorkerApp and Starter. Define as constant to avoid typos. |
| **Forgot `testWorkflow.getTestEnvironment().start()`** | Test hangs indefinitely or fails with timeout | Always call `.start()` before executing workflows in tests |
| **Used wrong dependency scope for temporal-testing** | Build errors in production; temporal-testing classes not found | Set `<scope>test</scope>` in pom.xml for temporal-testing dependency |
| **Activity method signature mismatch** | `io.temporal.failure.ApplicationFailure: Activity type not registered` | Ensure interface and implementation signatures match exactly (parameter types, return type) |
| **Forgot to register activities in Worker** | `Activity implementation not found` error | Call `worker.registerActivitiesImplementations(...)` in WorkerApp before starting |
| **Forgot to register workflow in Worker** | `Workflow type not registered` error | Call `worker.registerWorkflowImplementationTypes(YourWorkflowImpl.class)` in WorkerApp |
| **Imported wrong Workflow class** | Compile errors or weird behavior | Use `io.temporal.workflow.Workflow`, NOT your workflow interface class |
| **Activity stub created incorrectly** | `NullPointerException` when calling activity | Use `Workflow.newActivityStub(ActivityInterface.class, options)` as a class field, not local variable |
| **Used `@Autowired` in workflow/activities** | Spring annotations don't work in Temporal workers (unless using Spring Boot integration) | Manual dependency injection via constructors. No Spring in traditional pattern. |

### Debugging Tips

**If your workflow hangs forever:**
1. Check Temporal UI - is the workflow even registered?
2. Verify task queue names match in Worker and Starter
3. Check Worker logs - is it polling the correct task queue?
4. Is Temporal Server running? (`temporal server start-dev`)

**If activities fail with "not registered" errors:**
1. Check Worker logs - did you call `worker.registerActivitiesImplementations(...)`?
2. Are activity implementations instantiated correctly?
3. Do activity interface method signatures match implementations?

**If tests hang:**
1. Did you forget `testWorkflow.getTestEnvironment().start()`?
2. Check for infinite loops in workflow code
3. Enable debug logging: add `-Dtemporal.internal.worker.debug=true` to test JVM args

**If LLM API calls fail:**
1. Is `OPENAI_API_KEY` set correctly?
2. Try `DUMMY_MODE=true` to isolate Temporal issues from API issues
3. Check OpenAI API status: https://status.openai.com

**Enable debug logging:**
```xml
<!-- Add to src/main/resources/simplelogger.properties -->
org.slf4j.simpleLogger.log.io.temporal=DEBUG
```

---

## Testing the Solution

### Complete Terminal Workflow

Here's how to run and test your Temporal solution:

#### Terminal 1: Temporal Server

```bash
temporal server start-dev
```

Keep this running. Temporal UI available at http://localhost:8233

#### Terminal 2: Worker

```bash
cd exercise-901-githubpr/java
export OPENAI_API_KEY=sk-your-key-here  # Or: export DUMMY_MODE=true
mvn compile exec:java@worker
```

Expected output:
```
Worker started for task queue: github-pr-review
```

Keep this running. Worker polls for workflow tasks forever.

#### Terminal 3: Starter (Client)

```bash
cd exercise-901-githubpr/java
mvn compile exec:java@workflow
```

Expected output:
```
Starting workflow: pr-review-abc123...
============================================================
Overall Recommendation: APPROVE

Code Quality:
  Risk: LOW
  Recommendation: APPROVE
  Findings:
    - Function names are clear and descriptive
    - Code follows single responsibility principle
    - Error handling is present and appropriate

Test Quality:
  Risk: LOW
  Recommendation: APPROVE
  Findings:
    - Tests cover main functionality
    - Edge cases are tested
    - Test names are descriptive

Security:
  Risk: LOW
  Recommendation: APPROVE
  Findings:
    - No hardcoded secrets detected
    - Input validation is present
    - No SQL injection vulnerabilities found

Metadata:
  Generated: 2026-01-09T...
  Duration: 6234ms
  Model: gpt-4o-mini
============================================================
```

#### Terminal 4: Run Tests

```bash
cd exercise-901-githubpr/java
mvn test
```

Expected output:
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running solution.temporal.ReviewWorkflowTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

### Temporal UI Verification

Navigate to **http://localhost:8233**:

1. **Find your workflow:**
   - Click "Workflows" in left sidebar
   - Search for workflow ID (from Starter output)
   - Click to open workflow details

2. **Verify workflow execution:**
   - Status: "Completed"
   - Duration: ~6-9 seconds
   - No errors

3. **Inspect event history:**
   - `WorkflowExecutionStarted`
   - `WorkflowTaskScheduled` → `WorkflowTaskCompleted`
   - `ActivityTaskScheduled` (Code Quality)
   - `ActivityTaskStarted` → `ActivityTaskCompleted`
   - `ActivityTaskScheduled` (Test Quality)
   - `ActivityTaskStarted` → `ActivityTaskCompleted`
   - `ActivityTaskScheduled` (Security)
   - `ActivityTaskStarted` → `ActivityTaskCompleted`
   - `WorkflowExecutionCompleted`

4. **Click on each activity:**
   - See input payloads (prTitle, prDescription, diff)
   - See output payloads (AgentResult with recommendation, risk, findings)

### Manual Testing Scenarios

To fully verify your implementation, test these scenarios:

#### Scenario 1: All Agents Approve → Overall APPROVE

**Modify Starter to use a good PR:**
```java
request.prTitle = "Add logging to service layer";
request.diff = "+logger.info(\"Processing request for user: \" + userId);";
```

Expected: Overall = APPROVE

#### Scenario 2: One Agent Blocks → Overall BLOCK

**Modify Starter to trigger security block:**
```java
request.prTitle = "Add API key to config";
request.diff = "+const API_KEY = \"sk-abc123...\";  // Hardcoded secret!";
```

Expected: Overall = BLOCK (Security agent detects hardcoded secret)

#### Scenario 3: One Agent Requests Changes → Overall REQUEST_CHANGES

**Modify Starter to trigger test quality concern:**
```java
request.prTitle = "Add new validation logic";
request.diff = "+if (username.length < 3) throw new Error(\"Invalid username\");";
request.testSummary.totalTests = 0;  // No tests!
```

Expected: Overall = REQUEST_CHANGES (Test Quality agent detects missing tests)

#### Scenario 4: Simulate API Failure → Activities Retry

**Temporarily disable your internet or set invalid API key:**
```bash
export OPENAI_API_KEY=invalid-key
mvn compile exec:java@worker
# In another terminal:
mvn compile exec:java@workflow
```

Watch Temporal UI:
- Activities will fail initially
- Temporal automatically retries with exponential backoff
- After 5 attempts, workflow fails permanently (or succeeds if API recovers)

This demonstrates Temporal's automatic retry mechanism!

---

## Next Steps

Congratulations on completing this expert-level exercise! 🎉

### What You've Learned

1. ✅ **Temporal Testing** - How to write integration tests with TestWorkflowEnvironment
2. ✅ **Spring Boot Integration** - Traditional vs integrated patterns for Temporal + Spring Boot
3. ✅ **Multi-Agent Orchestration** - Coordinating multiple LLM agents with independent retry
4. ✅ **Activity Independence** - Why separate activities are better for cost and resilience
5. ✅ **Deterministic Aggregation** - Why routing logic belongs in workflows
6. ✅ **Retry Policies for LLMs** - Configuring aggressive retries for rate limits and timeouts

### How This Compares to Previous Exercises

**Exercises 01-06:** Introduction to Temporal fundamentals
- Basic workflow/activity patterns
- Retry policies
- Signals (exercise-06)

**Exercise 901:** Expert-level patterns
- Testing workflows (new!)
- Spring Boot integration strategies (new!)
- Production-grade multi-agent systems
- Real LLM API integration

### Production Considerations

To make this production-ready, consider:

1. **Spring Boot Integration:**
   - Use the integrated pattern (REST API + workers in one app)
   - Manage Temporal worker lifecycle with Spring

2. **Advanced Testing:**
   - Test all aggregation scenarios
   - Test activity retry behavior
   - Test workflow timeout handling
   - Mock LLM responses for deterministic tests

3. **Observability:**
   - Add structured logging
   - Export Temporal metrics to Prometheus
   - Set up alerts for workflow failures
   - Track LLM API costs

4. **Performance:**
   - Run activities in parallel (not sequential) using `Async.function()`
   - Cache LLM responses for identical PRs
   - Use cheaper models for simpler reviews

5. **Security:**
   - Store API keys in secrets manager (not env vars)
   - Validate PR inputs to prevent prompt injection
   - Rate limit review requests

### Resources

- **Temporal Docs:** https://docs.temporal.io
- **Java SDK Guide:** https://docs.temporal.io/dev-guide/java
- **Spring Boot Integration:** https://docs.temporal.io/develop/java/spring-boot-integration
- **Testing Guide:** https://docs.temporal.io/develop/java/testing
- **Temporal Samples:** https://github.com/temporalio/samples-java

---

**Need Help?**

- Stuck on implementation? Review the code snippets and patterns above
- Workflow not executing? Check common pitfalls section
- Tests failing? Re-read the Temporal Testing section
- Still confused? Review exercise-06 as a reference

**Happy Temporal coding!** 🚀
