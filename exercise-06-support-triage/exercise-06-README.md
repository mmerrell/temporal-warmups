# Exercise 06 - LLM-Powered Customer Support Triage (Java)

## Scenario

You have an AI-powered customer support triage system that coordinates multiple LLM agents:
1. **PII Scrubbing Agent** - Uses GPT-4 to redact sensitive information (credit cards, SSNs, emails)
2. **Classification Agent** - Uses GPT-4 to categorize and assess urgency

The system routes high-risk or low-confidence tickets to human review, while auto-processing low-risk tickets. This is a realistic use case for Temporal customers building **multi-agent AI orchestration** with **human-in-the-loop** approval.

## Run the Pre-Temporal Baseline

### Prerequisites

1. **Get OpenAI API Key**:
   - Sign up at https://platform.openai.com
   - Create an API key
   - Cost estimate: ~$0.02-0.05 per ticket (2 GPT-4 calls)

2. **Set environment variable**:
   ```bash
   export OPENAI_API_KEY=sk-your-key-here
   ```

3. **Run the baseline**:
   ```bash
   cd exercise-06-support-triage
   mvn compile exec:java
   ```

Watch what happens when either LLM agent fails (network issues, rate limits, etc.). The entire triage workflow fails - no retries, no recovery. If PII scrubbing succeeds but classification fails, you've wasted the first API call.

Expected output shows:
- PII scrubbing agent processing tickets
- Classification agent analyzing scrubbed text
- Occasional API failures (network timeouts, rate limits)
- Complete workflow failure when any agent fails

**Problems you'll observe:**
1. No retry logic - transient API failures cause permanent failures
2. No durability - process crash would lose all progress
3. No visibility - can't see what agent failed or why
4. Manual error handling - try/catch everywhere
5. Wasted API costs - must re-run BOTH agents if one fails
6. No human approval - high-risk tickets auto-processed
7. No audit trail - can't review AI decisions

## Your Task

Convert this multi-agent AI orchestration into a durable Temporal workflow with automatic retry handling and human-in-the-loop approval for high-risk tickets.

## Breaking Down the Problem

### Think About Activities vs Workflow

**Question:** Which operations in this code are non-deterministic (could produce different results or fail unpredictably)?

Look at the two main AI operations:
- `scrubPII(String ticketText)` - Calls OpenAI GPT-4 API (external service)
- `classifyTicket(String scrubbedText)` - Calls OpenAI GPT-4 API (external service)

**Question:** Why separate these into two activities instead of one combined activity?

Think about what should happen if classification fails but PII scrubbing succeeded. Should you:
- Re-run BOTH agents and pay for scrubbing again? (wasteful, expensive)
- Reuse the scrubbed text and retry just classification? (correct!)

This is why we need **two separate activities** - each can be retried independently, saving API costs and time.

### Multi-Agent AI Architecture

**Why Two Agents?**

1. **Independent Retry** - If classification fails, don't re-scrub PII
2. **Cost Efficiency** - Only pay for what fails
3. **Audit Trail** - See exactly which agent made which decision
4. **Separation of Concerns** - Each agent has a single responsibility

**Agent Responsibilities:**
- **PII Scrubber**: Security-focused, removes sensitive data
- **Classifier**: Business-focused, determines routing and urgency

### Pattern Recognition from Previous Exercises

If you completed Exercises 01-02, you learned about:
- The three-layer activity pattern (interface → implementation → business logic)
- Interface-based workflow design
- Activity stubs and retry configuration
- Worker and client setup

**Exercise 06 follows the same patterns**, but adds:
- Real external LLM API calls (not simulated)
- Multi-agent coordination (passing state between AI agents)
- Human-in-the-loop approval (new signal pattern!)

### Key Concepts to Apply

#### 1. Separating Non-Deterministic AI Operations

**Workflow code** must be deterministic:
- Same inputs always produce same outputs
- No calling external APIs directly
- No `System.currentTimeMillis()` or `new Random()` in workflow
- Use `Workflow.currentTimeMillis()` or `Workflow.newRandom()` instead

**Activity code** can be non-deterministic:
- Call external LLM APIs (OpenAI, Anthropic, etc.)
- Network operations with unpredictable latency
- API failures and retries

**Question:** Where does the routing decision belong - workflow or activity?

The routing logic (`if confidence < 0.7 || urgency == "critical"`) is **deterministic** - given the same classification, it always makes the same decision. This belongs in the **workflow**, not an activity!

#### 2. State Passing Between AI Agents

The workflow orchestrates the flow of data between agents:
```
Activity 1 (PII Scrubber) → returns scrubbed text
                         ↓
Activity 2 (Classifier) → takes scrubbed text, returns classification
                         ↓
Workflow Logic → makes routing decision (needs human review?)
```

The workflow stores no state itself - it just coordinates. Data flows through as parameters.

#### 3. Retry Policies for LLM APIs

LLM APIs have specific failure characteristics:
- **Rate limits** - Temporary (429 errors), need exponential backoff
- **Timeouts** - Network issues, transient
- **500 errors** - Server overload, temporary

Configure aggressive retry policies for LLM activities:
```java
RetryOptions.newBuilder()
    .setMaximumAttempts(5)              // More attempts than normal
    .setInitialInterval(Duration.ofSeconds(2))    // Start with 2s delay
    .setMaximumInterval(Duration.ofSeconds(60))   // Cap at 60s
    .setBackoffCoefficient(2.0)         // Exponential backoff
    .build()
```

#### 4. Human-in-the-Loop with Signals

**New Pattern**: Temporal Signals enable workflows to pause and wait for external input.

**Use case**: High-risk tickets need human approval before creating CRM case.

**How it works**:
1. Workflow executes PII scrubbing
2. Workflow executes classification
3. Workflow checks: `if (needsHumanReview) { wait for signal }`
4. External system sends signal: `workflow.approveTicket()`
5. Workflow resumes and creates CRM case

**Signal Definition** (in workflow interface):
```java
@WorkflowInterface
public interface SupportTriageWorkflow {
    @WorkflowMethod
    TriageResult triageTicket(String ticketId, String ticketText);

    @SignalMethod
    void approveTicket(boolean approved);
}
```

**Signal Wait** (in workflow implementation):
```java
if (needsHumanReview) {
    // Wait for human approval (workflow pauses here)
    Workflow.await(() -> approvalReceived);

    if (!approved) {
        return new TriageResult(false, "Rejected by human reviewer");
    }
}
```

This is a **powerful pattern** for compliance, risk management, and complex approvals!

#### 5. Type Serialization

Temporal serializes workflow inputs/outputs to JSON. Keep it simple:

**Works:**
- String, int, long, boolean, double
- Simple POJOs with empty constructors
- Lists, arrays

**Question:** What data structure should your workflow return?
- Just a boolean (success/failure)?
- A result object with classification, case ID, and review status?

Recommended: Use a POJO like `TriageResult` with all relevant data for observability.

## What to Observe When Testing

After implementing your Temporal version:

1. **Automatic Retries:**
   - Run your worker and client
   - If OpenAI API fails (rate limit, timeout), watch Temporal retry automatically
   - PII scrubbing is NOT re-run if only classification fails
   - Check worker logs to see retry attempts

2. **Temporal UI:**
   - Open http://localhost:8233
   - Find your workflow execution
   - See both activities in the timeline
   - Look at Event History - see retry attempts
   - Notice how scrubbed text is passed from Activity 1 to Activity 2

3. **Durability:**
   - Start a workflow execution
   - Kill the worker process (Ctrl+C) mid-execution
   - Restart the worker
   - Watch the workflow resume from last completed activity
   - No data lost, no API calls re-run unnecessarily

4. **Human-in-the-Loop:**
   - Run a high-risk ticket (low confidence or critical urgency)
   - Workflow pauses waiting for signal
   - Send approval signal from client
   - Workflow resumes and completes

5. **Cost Efficiency:**
   - Failed classification doesn't re-run PII scrubbing
   - Each agent call is tracked independently
   - Full audit trail of which agent made which decision

## OpenAI API Setup

### Get API Key

1. Go to https://platform.openai.com
2. Sign up or log in
3. Navigate to API Keys section
4. Create new secret key
5. Copy the key (starts with `sk-`)

### Set Environment Variable

```bash
# macOS/Linux
export OPENAI_API_KEY=sk-your-key-here

# Windows
set OPENAI_API_KEY=sk-your-key-here
```

### Cost Estimates

- GPT-4 input: ~$0.03 per 1K tokens
- GPT-4 output: ~$0.06 per 1K tokens
- Typical ticket: 100-200 tokens input, 50-100 tokens output
- **Per ticket cost**: ~$0.02-0.05 (2 API calls)
- **5 test tickets**: ~$0.10-0.25

This is a learning exercise - total cost should be under $1 for all testing.

## Success Criteria

- Both activities execute sequentially
- Workflows complete successfully (check Temporal UI at http://localhost:8233)
- Automatic retries handle OpenAI API failures
- PII scrubbing not re-run when only classification fails
- No manual retry logic in your code
- High-risk tickets pause for signal (human approval)
- Can complete the exercise in **under 60 minutes** (first time)

## Mental Model Shift

**Before Temporal:**
> "If classification fails, everything fails. Must re-run PII scrubbing too. Wasted $0.02 API call. Can't review high-risk tickets - they auto-process. If process crashes, start from scratch."

**After Temporal:**
> "PII scrubbed once. If classification fails, Temporal retries just that step. No wasted API calls. High-risk tickets pause for human approval via signals. If process crashes, workflow resumes from last completed activity. Complete AI decision audit trail in Temporal UI."

## Common Pitfalls

1. **Forgetting to set OPENAI_API_KEY**
   - Error: `NullPointerException` or `401 Unauthorized`
   - Solution: `export OPENAI_API_KEY=sk-...`

2. **Not handling JSON parsing from OpenAI responses**
   - OpenAI returns structured text, not always valid JSON
   - Use simple text parsing (like in baseline) or robust JSON parsing

3. **Putting routing logic in activities instead of workflow**
   - ❌ Activity that calls LLM AND makes routing decision
   - ✅ Activity returns classification, workflow makes routing decision
   - Why? Workflow logic is deterministic and should live in workflow

4. **Using non-deterministic functions in workflow code**
   - ❌ `System.currentTimeMillis()` in workflow
   - ✅ `Workflow.currentTimeMillis()` in workflow

5. **Not configuring retry policies**
   - Activities won't retry without explicit `RetryOptions`
   - LLM APIs need aggressive retries (rate limits are common)

6. **Forgetting empty constructors in POJOs**
   - Temporal needs them for deserialization
   - Add `public ClassName() {}` to all data classes

7. **Mismatched task queue names**
   - Worker and client must use same task queue
   - Pick one name (e.g., "support-triage") and use consistently

8. **Signal timeout handling**
   - What if human never approves? Workflow waits forever
   - Use `Workflow.await(Duration, condition)` with timeout
   - Example: `Workflow.await(Duration.ofHours(24), () -> approvalReceived)`

## Implementation Path

You'll need to create several components:

### Domain Models
- `TicketClassification` - Result from classification agent (category, urgency, confidence, reasoning)
- `TriageResult` - Final workflow result (success, classification, caseId, needsHumanReview)

### Activities (Interface + Implementation)
1. **PIIScrubberActivity**
   - Interface: `String scrubPII(String ticketText);`
   - Implementation: Calls OpenAI GPT-4 with PII scrubbing prompt

2. **ClassificationActivity**
   - Interface: `TicketClassification classifyTicket(String scrubbedText);`
   - Implementation: Calls OpenAI GPT-4 with classification prompt

### Workflow (Interface + Implementation)
- Interface: Define workflow method and signal method
- Implementation:
  - Execute PII scrubbing activity
  - Execute classification activity
  - Make routing decision (deterministic)
  - Wait for signal if high-risk
  - Create CRM case (log/print)

### Worker
- Register workflow implementation
- Register activity implementations
- Connect to Temporal server

### Client/Starter
- Create workflow stub
- Execute workflow with sample tickets
- (Optional) Send approval signals

**Hint:** Look at Exercise 02's structure. The patterns are identical, just with OpenAI API calls instead of email simulation.

## Code Snippet Examples

### Activity Interface Pattern

```java
@ActivityInterface
public interface PIIScrubberActivity {
    @ActivityMethod
    String scrubPII(String ticketText);
}
```

### Activity Options with Aggressive Retries for LLMs

```java
ActivityOptions options = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofMinutes(2))
    .setRetryOptions(RetryOptions.newBuilder()
        .setMaximumAttempts(5)
        .setInitialInterval(Duration.ofSeconds(2))
        .setMaximumInterval(Duration.ofSeconds(60))
        .setBackoffCoefficient(2.0)
        .build())
    .build();
```

### Creating Activity Stub in Workflow

```java
private final PIIScrubberActivity piiScrubber =
    Workflow.newActivityStub(PIIScrubberActivity.class, options);
```

### Signal Definition

```java
@WorkflowInterface
public interface SupportTriageWorkflow {
    @WorkflowMethod
    TriageResult triageTicket(String ticketId, String ticketText);

    @SignalMethod
    void approveTicket(boolean approved);
}
```

### Signal Wait with Timeout

```java
// In workflow implementation
boolean signalReceived = Workflow.await(
    Duration.ofHours(24),
    () -> approvalReceived
);

if (!signalReceived) {
    // Timeout - auto-reject or escalate
    return new TriageResult(false, "Timeout waiting for approval");
}
```

### Sending Signal from Client

```java
// Get workflow stub by ID
SupportTriageWorkflow workflow = client.newWorkflowStub(
    SupportTriageWorkflow.class,
    workflowId
);

// Send approval signal
workflow.approveTicket(true);
```

These are patterns, not complete implementations. Discover the full structure by applying these concepts!

## Real-World Applications

This pattern applies to many Temporal customer use cases:

1. **Content Moderation** - AI screening → human review for edge cases
2. **Document Processing** - AI extraction → human verification
3. **Fraud Detection** - AI risk scoring → analyst approval for high-risk
4. **Resume Screening** - AI filtering → recruiter review for qualified candidates
5. **Medical Diagnosis** - AI analysis → doctor approval before treatment
6. **Financial Trading** - AI strategy → compliance approval for large trades

**Common Thread**: Multi-agent AI orchestration with human-in-the-loop approval for high-risk decisions.

## What's Next?

After completing this exercise, you'll understand:
- How to orchestrate multiple LLM agents with Temporal
- Cost-efficient retry strategies (only retry what failed)
- Human-in-the-loop approval patterns with signals
- Full audit trail of AI decisions for compliance
- Durable AI workflows that survive crashes

**Exercise #7** could introduce:
- Parallel AI agent execution (fan-out/fan-in)
- Dynamic workflows (variable number of AI agents)
- Continue-as-new for long-running AI pipelines

But now you have the foundation for **production-grade AI orchestration**!

---

**Status**: Complete ✅
**Language**: Java
**Concepts**: Multi-agent AI, LLM orchestration, human-in-the-loop, signals, retry policies, cost efficiency
**Difficulty**: Medium-Advanced (3/5)
**Estimated Time**: ~60 minutes
**Prerequisites**: Exercises 01-02, OpenAI API key
