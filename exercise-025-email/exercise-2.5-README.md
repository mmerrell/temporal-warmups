# Exercise 2.5 - Email Verification Flow

## Scenario
A minimal two-step email verification workflow for new user signups. This is a **muscle memory builder** - intentionally simple and clean to practice the basic Temporal pattern without debugging distractions.

## Original Code
See `original/email_verification.py` - a class-based implementation with:
- 2 steps: generate verification token, send email
- Simulated email service failures (10% failure rate)
- No retry logic
- Simple try/except error handling

## Learning Goals
- **Build muscle memory** for the basic workflow → activity pattern
- Practice the setup rhythm: models → activities → workflow → worker → client
- Get comfortable with the Temporal development loop
- Reinforce retry policy configuration
- Speed: Target 20-30 minutes (down from 1+ hour in Exercise #1)

## Key Concepts
- **Minimal Complexity**: Only 2 activities, focus on the pattern
- **Clean Starting Code**: No messy refactoring, just straightforward conversion
- **Retry Policies**: Same pattern as Exercise #1 and #2
- **State Passing**: Token generated in activity 1, used in activity 2

## Running the Exercise

### Prerequisites
**Use Python 3.9 or 3.10** (learned from Exercise #2!)

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
exercise-2.5-email-verification/
├── original/
│   └── email_verification.py    # Starting code (before Temporal)
├── python/
│   ├── activities.py             # generate_token, send_verification_email
│   ├── workflow.py               # EmailVerificationWorkflow
│   ├── worker.py                 # Worker setup
│   ├── client.py                 # Test client
│   └── requirements.txt
└── README.md
```

## What to Observe

1. **Simple Sequential Flow**: Token generation → Email sending
2. **Automatic Retries**: Email service fails ~10% of the time, watch Temporal retry
3. **Clean Logs**: Worker shows each step executing
4. **Temporal UI**: 
   - See 2 activities per workflow
   - Check retry attempts when email fails
   - View execution timeline

## Key Differences from Exercise #1 & #2

**Simpler than Exercise #1:**
- Only 2 activities (vs 3 in Exercise #1, 5 in Exercise #2)
- No external database needed
- Straightforward state passing (just a token string)

**Cleaner than Exercise #2:**
- No debugging nightmares
- No complex data structures
- No inventory/payment coordination
- Just pure pattern practice

## Common Patterns Reinforced

### 1. Activity Definition
```python
@activity.defn
async def generate_token(email: str) -> str:
    # Non-deterministic operations (secrets.token_urlsafe)
    # Can use time.sleep, random, etc.
    return token
```

### 2. Workflow Orchestration
```python
@workflow.defn
class EmailVerificationWorkflow:
    @workflow.run
    async def run(self, email: str):
        # Deterministic orchestration logic
        token = await workflow.execute_activity(...)
        link = await workflow.execute_activity(...)
        return result
```

### 3. Import Pattern
```python
# workflow.py
from temporalio import workflow

# Only wrap activities
with workflow.unsafe.imports_passed_through():
    from activities import generate_token, send_verification_email
```

### 4. Worker Registration
```python
worker = Worker(
    client,
    workflows=[EmailVerificationWorkflow],
    task_queue="email-verification-tasks",
    activities=[generate_token, send_verification_email]
)
```

### 5. Client Execution
```python
result = await client.execute_workflow(
    EmailVerificationWorkflow.run,
    args=[email],
    id=f"verify-{email}-{uuid.uuid4()}",
    task_queue="email-verification-tasks",
)
```

## Success Criteria

You've successfully completed this exercise when:
- ✅ Both activities execute in sequence
- ✅ Workflows complete successfully (check Temporal UI)
- ✅ Automatic retries happen when email service fails
- ✅ You can run the entire flow in **under 30 minutes**
- ✅ Worker logs show clean execution flow

## Optional Enhancements

If you finish quickly and want extra practice:

1. **Add type hints everywhere**:
   ```python
   async def run(self, email: str) -> dict:
   ```

2. **Return structured results**:
   ```python
   return {
       'success': True,
       'email': email,
       'token': token,
       'link': link
   }
   ```

3. **Add input validation in workflow**:
   ```python
   if not email or '@' not in email:
       raise ValueError("Invalid email")
   ```

4. **Capture and print results in client**:
   ```python
   result = await client.execute_workflow(...)
   print(f"✓ {email}: {result}")
   ```

## Time Tracking

**Target**: 20-30 minutes  
**Actual**: ~45 minutes (first attempt after Exercise #2)

**Expected progression:**
- First time: 30-45 minutes
- Second time: 20-30 minutes
- Third time: 15-20 minutes

This is about building **speed and confidence** with the basic pattern.

## What's Next?

**Exercise #3** will introduce:
- Messier starting code (80+ line function to refactor)
- First compensation pattern (rollback/saga)
- Inconsistent error handling to clean up
- More complex decision-making about activity boundaries

But now you have the basic pattern **locked in**. You can spin up a 2-activity workflow in under 30 minutes!

---

**Status**: Complete ✅  
**Language**: Python  
**Concepts**: Basic workflow pattern, muscle memory, speed building  
**Difficulty**: Easy (1/5)  
**Actual Time**: ~45 minutes (first attempt)
