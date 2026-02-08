# Temporalizing Strategy

## 1. Create the workflow

Call `public ReviewResponse review(ReviewRequest request)`

Follow execution steps in `ReviewOrchestrator.java` to finish the workflow

### What is required in a workflow?

- Only deterministic activities

1. Implements interface
2. configure how activities should behave - timeouts, retries
3. create activity stubs with non-existent interfaces. Using Workflow
4. call the workflow method

## 2. Create the Starter using a 6-step process

## 3. Create Worker using 8-step process

This is the more involved step. It will require creating the `ActivityImpl` and the business logic objects.

## 4. (Optional) Update POM.xml for simple CLI execution

```bash
# Terminal 2: Temporal Worker
mvn compile exec:java@worker

# Terminal 3: Temporal Client (Starter)
mvn compile exec:java@workflow
```