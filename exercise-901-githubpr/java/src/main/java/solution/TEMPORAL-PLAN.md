# Temporalizing the solution

## 1. Create the workflow

Call `public ReviewResponse review(ReviewRequest request)`

Follow execution steps in `ReviewOrchestrator.java` to finish the workflow

### What is required in a workflow?

- Only deterministic activities

1. Implements interface
2. // 1. configure how activities should behave - timeouts, retries
3.     //2. create activity stubs with non-existent interfaces. Using Workflow
4.     // 3. call the workflow method
5. 



## The Activities

```java
// first call
AgentResult codeQuality = codeQualityAgent.analyze(
    request.prTitle,
    request.prDescription,
    request.diff
);
```

```java
//2nd call
AgentResult testQuality = testQualityAgent.analyze(
    request.prTitle,
    request.prDescription,
    request.diff,
    request.testSummary
);

```

```java
// 3. Call Security Agent (BLOCKS for ~2-3 seconds)
System.out.println("[3/3] Calling Security Agent...");
AgentResult security = securityAgent.analyze(
    request.prTitle,
    request.prDescription,
    request.diff
);
```

```java
            // 5. Build response
            long tookMs = System.currentTimeMillis() - startMs;
```

```java
            Metadata metadata = new Metadata(
                Instant.now().toString(),
                tookMs,
                System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini")
            );
```

## 2. Create the Starter using a 6-step process

## 3. Create Worker using 8-step process

This is the more involved step. It will require creating the `ActivityImpl` and the business logic objects.

## 4. (Optional) Update POM.xml for simple CLI execution