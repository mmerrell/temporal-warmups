# Exercise 01 - User Registration

## Scenario
A simple user registration flow with validation, database creation, and email notifications. The original code runs sequentially with no durability or retry logic.

## Original Code
See `original/registration_service.py` - a class-based implementation with:
- 4 steps: validate, create user, send welcome email, send verification email
- Simulated failures (10% database, 15% email)
- No retry logic
- No state recovery

## Learning Goals
- Separate workflow orchestration from activities
- Implement retry policies
- See automatic failure recovery
- Understand durability (workflow survives worker restarts)

## Key Concepts
- **Workflow**: Orchestrates the 4 steps, deterministic
- **Activities**: Each step is an activity (non-deterministic operations)
- **External State**: Database is external to workflows
- **Retry Policies**: Automatic retries on transient failures

## Running the Exercise

1. **Start Temporal**:
```bash
   temporal server start-dev
```

2. **Install dependencies**:
```bash
   cd python
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

## What to Observe
- Workflows appear in UI with unique IDs
- Activity retries happen automatically on failure
- Worker can be killed mid-workflow and will resume when restarted
- State (user_id) is preserved between activities

## Time Estimate
~60 minutes (including debugging)

## Next Steps
Exercise #2 will add messier starting code and introduce compensation patterns.
