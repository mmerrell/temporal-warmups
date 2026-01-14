# Bad Java AI Agent Orchestrator (NO TEMPORAL)
Spring Boot + Maven + OpenAI

This document defines an intentionally **brittle, synchronous, and unscalable** Java service.
It is the **BEFORE** state that will later be Temporalized by the developer.

The **agent logic must be real, valuable, and production‑relevant**.
The **orchestration must be naive and fragile on purpose**.

---

## Non‑negotiables
- Use **Spring Boot**
- Use **Maven (mvn)**
- Use **real OpenAI API calls**
- Make the LLM client easy to swap later
- Do **NOT** use Temporal
- Do **NOT** add retries, queues, schedulers, async execution, persistence, or background workers
- Serial execution only
- Fail fast on errors

This service should work — but clearly **not scale**.

---

## HTTP API

### Endpoint
`POST /review`

### Request JSON
```json
{
  "prTitle": "string",
  "prDescription": "string",
  "diff": "string",
  "testSummary": {
    "passed": true,
    "totalTests": 0,
    "failedTests": 0,
    "durationMs": 0
  }
}
```

- `testSummary` is optional
- If missing, assume `passed=true` and zeros

### Response JSON
```json
{
  "overallRecommendation": "APPROVE|REQUEST_CHANGES|BLOCK",
  "agents": [
    {
      "agentName": "Code Quality",
      "riskLevel": "LOW|MEDIUM|HIGH",
      "recommendation": "APPROVE|REQUEST_CHANGES|BLOCK",
      "findings": ["string", "..."]
    },
    {
      "agentName": "Test Quality",
      "riskLevel": "LOW|MEDIUM|HIGH",
      "recommendation": "APPROVE|REQUEST_CHANGES|BLOCK",
      "findings": ["string", "..."]
    },
    {
      "agentName": "Security",
      "riskLevel": "LOW|MEDIUM|HIGH",
      "recommendation": "APPROVE|REQUEST_CHANGES|BLOCK",
      "findings": ["string", "..."]
    }
  ],
  "metadata": {
    "generatedAt": "ISO-8601 string",
    "tookMs": 0,
    "model": "string"
  }
}
```

---

## Orchestration Behavior (INTENTIONALLY BAD)

Inside the `/review` controller:

1. Run **Code Quality Agent**
2. Then **Test Quality Agent**
3. Then **Security Agent**
4. Aggregate results
5. Return response

Constraints:
- Entire flow is synchronous
- HTTP request thread blocks
- Each agent makes its own OpenAI call
- If any agent fails or JSON parsing fails → return HTTP 500
- No retries
- No timeouts beyond default HTTP client behavior
- No persistence
- No idempotency
- No observability
- Minimal logging (`System.out.println` is fine)

Add TODO comments explaining why this is bad:
- no retries
- no durability
- no observability
- unbounded latency
- cannot resume after failure
- unsafe for scale

---

## OpenAI Integration

### LLM abstraction
Create a minimal interface:
```java
interface LlmClient {
  String chat(List<Message> messages, LlmOptions options);
}
```

### Implementation
- `OpenAiLlmClient`
- All OpenAI logic lives here
- Use HTTP client (WebClient or OkHttp)
- Require JSON‑only responses

### Configuration (env vars)
- `OPENAI_API_KEY` (required)
- `OPENAI_MODEL` (default to a GPT‑4‑class model)
- `OPENAI_BASE_URL` (default OpenAI endpoint)

If JSON parsing fails → throw exception.

---

## Agent Responsibilities (REAL LOGIC)

### 1. Code Quality Agent
- Uses **predefined criteria supplied externally**
- Evaluates naming, function size, responsibilities, boundaries, error handling, and refactoring opportunities
- Findings must reference concrete issues in the diff
- No generic fluff
- Recommendation must align with severity

Do **not** redefine the criteria here.

---

### 2. Test Quality Agent (simple but useful)
This agent assesses whether the diff is adequately tested.

Rules:
- If `testSummary.passed == false` → BLOCK
- If diff introduces new logic, branching, validation, or API behavior without tests → REQUEST_CHANGES
- If diff appears to be refactor/comments only → APPROVE
- If changes affect auth, validation, or error handling without tests → REQUEST_CHANGES

Output must include:
- Clear findings
- Exactly **3 high‑value suggested tests**

---

### 3. Security Agent (realistic rules)
This agent looks for practical application security issues.

Rules:
1. **Secrets & sensitive data**
    - Detect logging, returning, or storing secrets/tokens
    - Risk HIGH if secrets appear directly

2. **Authn/Authz correctness**
    - Detect changes that weaken or bypass authorization
    - Missing or inconsistent access control → BLOCK

3. **Injection & unsafe input handling**
    - Detect SQL injection, command injection, SSRF, unsafe deserialization, path traversal
    - Risk HIGH if dangerous sinks lack validation

Recommendation logic:
- Auth bypass or injection risk → BLOCK
- Questionable secrets handling → REQUEST_CHANGES
- Otherwise → APPROVE

---

## Aggregation Logic
- If ANY agent returns BLOCK → overall BLOCK
- Else if ANY returns REQUEST_CHANGES → overall REQUEST_CHANGES
- Else → APPROVE

---

## Dummy Mode (for early dev)
Add a flag:
`DUMMY_MODE=true`

When enabled:
- Skip OpenAI calls
- Return deterministic canned agent responses

---

## Project Structure (suggested)
```
controller/ReviewController.java
service/ReviewOrchestrator.java
llm/LlmClient.java
llm/OpenAiLlmClient.java
agents/CodeQualityAgent.java
agents/TestQualityAgent.java
agents/SecurityAgent.java
model/AgentInput.java
model/TestSummary.java
model/AgentResult.java
model/AgentReviewResult.java
```

---

## Tests (minimal)
1. Unit test: one test only, aggregation logic returns BLOCK if any agent blocks
2. Controller test (MockMvc): DUMMY_MODE returns 200 and valid JSON shape

No resilience libraries. No retries.

---

## Reminder
This is intentionally the **wrong architecture**.

The agents should be good.  
The workflow should be bad.

This service exists only so it can be replaced by Temporal.
