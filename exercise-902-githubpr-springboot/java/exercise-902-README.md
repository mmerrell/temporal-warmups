# Exercise 902 - Spring Boot Integration ⭐⭐⭐ EXPERT LEVEL

## Scenario

In Exercise 901, you built a working Temporal solution using **plain Java** - a standalone `WorkerApp.java` and `Starter.java`. This was great for learning Temporal fundamentals, but real production systems need more:

- **REST API endpoints** - Workflows triggered via HTTP, not manual `main()` calls
- **Dependency injection** - Cleaner code, easier testing, better architecture
- **Configuration management** - Environment-based config (dev/staging/prod)
- **Health checks** - Required for Kubernetes deployments
- **Production deployment** - Docker + K8s + monitoring

**Your task:** Transform Exercise 901's standalone Java into a **production-ready Spring Boot application** that integrates Temporal workers with REST APIs, dependency injection, and configuration management.

### Why This Exercise is Expert-Level ⭐⭐⭐

This exercise teaches you **production Spring Boot + Temporal patterns**:

1. **Temporal Worker as Spring Component** - Lifecycle management with `@PostConstruct` and `@PreDestroy`
2. **REST API Integration** - Triggering workflows via HTTP endpoints
3. **Dependency Injection for Activities** - Clean separation of concerns
4. **Async Workflow Execution** - Non-blocking HTTP requests
5. **Configuration via application.yml** - Environment-based config

These are **critical skills** for building production Temporal applications with Spring Boot.

### Problems with Exercise 901's Approach

Exercise 901's plain Java solution has these limitations:

1. **No REST API** - Must run `Starter.main()` manually for each PR review
2. **No Dependency Injection** - Manual `new CodeQualityAgent()` everywhere, hard to test
3. **No Configuration** - Hardcoded values (`"localhost"`, `7233`, `"pr-review"`) scattered throughout code
4. **No Health Checks** - Can't deploy to Kubernetes without `/actuator/health` endpoint
5. **Awkward Architecture** - Separate processes for worker and client, hard to manage
6. **Not Production-Ready** - Can't scale, monitor, or deploy easily

**Spring Boot solves all of these problems** through dependency injection, REST APIs, and production-ready features.

---

## What's Included ✅

**All code from Exercise 901 is already here!** The folder structure:

```
src/main/java/
├── exercise/                    # Pre-Temporal baseline (from 901)
│   ├── agents/                  # Business logic agents
│   ├── llm/                     # OpenAI client
│   ├── model/                   # Domain models (ReviewRequest, etc.)
│   └── ...
│
└── solution/temporal/           # Your workspace
    ├── PRReviewWorkflow.java    # ✅ Already included (from 901)
    ├── PRReviewWorkflowImpl.java # ✅ Already included
    ├── *Activity*.java          # ✅ Already included (6 files)
    ├── WorkerApp.java           # ✅ Plain Java worker (901)
    ├── Starter.java             # ✅ Plain Java starter (901)
    │
    └── TODO: Add Spring Boot integration (5 new files)
        - Application.java       # Spring Boot entry point
        - TemporalConfig.java    # Configuration beans
        - TemporalWorker.java    # Worker component
        - PRReviewController.java # REST API
        - WorkflowExecutionResponse.java # DTO
```

**You only need to create 5 new files** - the Spring Boot wrapper around existing Temporal code!

---

## Prerequisites

Before starting, ensure you have:

1. **Completed Exercise 901** - Understanding of workflows/activities
2. **Temporal Server Running:**
   ```bash
   temporal server start-dev
   ```
3. **OpenAI API Key** (or use DUMMY_MODE):
   ```bash
   export DUMMY_MODE=true
   ```
4. **Java 11+ and Maven:**
   ```bash
   java -version   # Should be 11 or higher
   mvn --version
   ```

---

## Architecture Transformation

### BEFORE (Exercise 901 - Plain Java)

```
┌─────────────────┐       ┌──────────────────┐
│   Starter.java  │──────▶│  WorkerApp.java  │
│  (Separate)     │       │   (Separate)     │
│                 │       │                  │
│ • Manual client │       │ • Manual DI      │
│ • Run once      │       │ • new Agent()    │
│ • No REST API   │       │ • Runs forever   │
└─────────────────┘       └──────────────────┘
```

**Problems:**
- Two separate Java processes to manage
- No way to trigger workflows via HTTP
- Manual dependency creation
- Hard to deploy to production

### AFTER (Exercise 902 - Spring Boot)

```
┌───────────────────────────────────────────┐
│      Spring Boot Application              │
│      (Single Unified Process)             │
│                                           │
│  ┌────────────────────────────┐          │
│  │   REST Controller          │          │
│  │   POST /api/review         │          │
│  │   GET /api/review/{id}     │          │
│  │   GET /actuator/health     │          │
│  └──────────┬─────────────────┘          │
│             │                            │
│             │ WorkflowClient             │
│             ▼                            │
│  ┌────────────────────────────┐          │
│  │   Temporal Worker          │          │
│  │   (@Component)             │          │
│  │                            │          │
│  │ • Auto-started             │          │
│  │ • Spring DI                │          │
│  │ • @PostConstruct           │          │
│  └────────────────────────────┘          │
└───────────────────────────────────────────┘
```

**Benefits:**
- Single Spring Boot application
- REST API for workflow execution
- Dependency injection throughout
- Health checks for Kubernetes
- Configuration via application.yml
- Production-ready

---

## Your Task

### Goal

Create a Spring Boot application that:

1. **Starts a Temporal worker automatically** when the application starts
2. **Exposes REST endpoints** to submit PR reviews and retrieve results
3. **Uses dependency injection** for all agents and activities
4. **Manages configuration** via `application.yml`
5. **Provides health checks** for Kubernetes readiness
6. **Executes workflows asynchronously** (non-blocking HTTP requests)

### What's Already Included ✅

**All code from Exercise 901 is already included!** You don't need to copy anything:

**From `exercise/` package (pre-Temporal baseline):**
- ✅ All agents (`CodeQualityAgent`, `TestQualityAgent`, `SecurityAgent`)
- ✅ All models (`ReviewRequest`, `ReviewResponse`, `AgentResult`, etc.)
- ✅ LLM client (`OpenAiLlmClient`, etc.)
- ✅ Original Spring Boot app and controller (for reference)

**From `solution/temporal/` package (901's Temporal solution):**
- ✅ `PRReviewWorkflow.java` (workflow interface)
- ✅ `PRReviewWorkflowImpl.java` (workflow implementation)
- ✅ All 6 activity files (interfaces + implementations)
- ✅ `WorkerApp.java` (plain Java worker from 901)
- ✅ `Starter.java` (plain Java starter from 901)

### What to Create (New Spring Boot Integration Layer)

You need to create these **5 new Spring Boot files** in `solution/temporal/`:

1. **`Application.java`** - New Spring Boot entry point (different from exercise/Application.java)
2. **`TemporalConfig.java`** - Temporal client/worker configuration beans
3. **`TemporalWorker.java`** - Worker component with lifecycle management
4. **`PRReviewController.java`** - New REST API controller (different from exercise/ReviewController.java)
5. **`WorkflowExecutionResponse.java`** - Response DTO for async workflow submission

**Note:** `pom.xml` and `application.yml` are already created!

**Total:** Just 5 new files to create - all the Temporal logic from 901 is already here!

---

## Key Components to Implement

### 1. Spring Boot Entry Point

**File:** `Application.java`

**What it should do:**
- Use `@SpringBootApplication` annotation
- Have a `main()` method that calls `SpringApplication.run()`
- This starts the entire application (worker + REST API) in one process

**Requirements:**
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

### 2. Temporal Configuration

**File:** `TemporalConfig.java`

**What it should do:**
- Use `@Configuration` annotation to define Spring beans
- Create `@Bean` for `WorkflowServiceStubs` (connects to Temporal server)
- Create `@Bean` for `WorkflowClient` (client for executing workflows)
- Create `@Bean` for each agent (CodeQualityAgent, TestQualityAgent, SecurityAgent)
- Use `@Value` to read configuration from `application.yml`

**Key beans to create:**
```java
@Configuration
public class TemporalConfig {
    @Value("${temporal.host:localhost}")
    private String temporalHost;

    @Value("${temporal.port:7233}")
    private int temporalPort;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        // TODO: Connect to Temporal server
        // Use WorkflowServiceStubs.newLocalServiceStubs()
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        // TODO: Create WorkflowClient
        // Use WorkflowClient.newInstance(serviceStubs)
    }

    @Bean
    public CodeQualityAgent codeQualityAgent() {
        // TODO: Create and return CodeQualityAgent instance
    }

    // TODO: Add beans for TestQualityAgent and SecurityAgent
}
```

**Why this matters:** Spring will automatically inject these beans wherever needed, eliminating manual `new` calls.

---

### 3. Temporal Worker Component

**File:** `TemporalWorker.java`

**What it should do:**
- Use `@Component` annotation so Spring manages it
- Use `@Autowired` constructor injection for `WorkflowClient` and all agents
- Use `@PostConstruct` to start the worker when Spring starts the application
- Use `@PreDestroy` to shutdown the worker gracefully when application stops
- Register your workflow implementation (`PRReviewWorkflowImpl`)
- Register all 3 activity implementations with agent dependencies

**Structure:**
```java
@Component
public class TemporalWorker {
    private final WorkflowClient client;
    private final CodeQualityAgent codeQualityAgent;
    private final TestQualityAgent testQualityAgent;
    private final SecurityAgent securityAgent;
    private WorkerFactory factory;

    @Autowired
    public TemporalWorker(
        WorkflowClient client,
        CodeQualityAgent codeQualityAgent,
        TestQualityAgent testQualityAgent,
        SecurityAgent securityAgent
    ) {
        // TODO: Store injected dependencies
    }

    @PostConstruct
    public void start() {
        // TODO: Create WorkerFactory
        // TODO: Create Worker for task queue "pr-review"
        // TODO: Register PRReviewWorkflowImpl
        // TODO: Register all 3 activity implementations
        // TODO: Start the factory
    }

    @PreDestroy
    public void shutdown() {
        // TODO: Shutdown factory gracefully
    }
}
```

**Key methods:**
- `WorkerFactory.newInstance(client)` - creates factory
- `factory.newWorker("pr-review")` - creates worker
- `worker.registerWorkflowImplementationTypes(...)` - registers workflow
- `worker.registerActivitiesImplementations(...)` - registers activities
- `factory.start()` - starts the worker

**Why `@PostConstruct`?** This method runs automatically after Spring creates the bean, starting your worker without manual intervention.

---

### 4. REST API Controller

**File:** `PRReviewController.java`

**What it should do:**
- Use `@RestController` and `@RequestMapping("/api")` annotations
- Inject `WorkflowClient` via constructor
- Implement **POST /api/review** endpoint:
  - Accept `@RequestBody ReviewRequest`
  - Generate unique workflow ID
  - Create workflow stub with `WorkflowOptions`
  - **CRITICAL:** Start workflow asynchronously using `WorkflowClient.start()`
  - Return HTTP 202 Accepted with workflow ID immediately (don't wait!)
- Implement **GET /api/review/{workflowId}** endpoint:
  - Accept workflow ID as `@PathVariable`
  - Reconnect to workflow using stub
  - Return result if available, 404 if not found

**Structure:**
```java
@RestController
@RequestMapping("/api")
public class PRReviewController {
    private final WorkflowClient client;

    @Autowired
    public PRReviewController(WorkflowClient client) {
        this.client = client;
    }

    @PostMapping("/review")
    public ResponseEntity<WorkflowExecutionResponse> submitReview(
        @RequestBody ReviewRequest request
    ) {
        // TODO: Generate workflow ID (use UUID)
        // TODO: Create workflow stub with WorkflowOptions
        // TODO: Start workflow ASYNCHRONOUSLY using WorkflowClient.start()
        // TODO: Return 202 Accepted with workflow ID
    }

    @GetMapping("/review/{workflowId}")
    public ResponseEntity<ReviewResponse> getReviewResult(
        @PathVariable String workflowId
    ) {
        // TODO: Reconnect to workflow using stub
        // TODO: Get result (this blocks until workflow completes)
        // TODO: Return result or 404
    }
}
```

**CRITICAL: Why Async?**

❌ **DON'T DO THIS** (blocks HTTP thread for 6-9 seconds):
```java
ReviewResponse result = workflow.review(request);  // BLOCKS!
return ResponseEntity.ok(result);
```

✅ **DO THIS** (returns immediately):
```java
WorkflowClient.start(workflow::review, request);  // Async!
return ResponseEntity.accepted()
    .body(new WorkflowExecutionResponse(workflowId, "RUNNING"));
```

---

### 5. Response DTO for Async Submission

**File:** `WorkflowExecutionResponse.java`

**What it should do:**
- Simple POJO for returning workflow ID and status
- Used by POST /api/review endpoint

**Structure:**
```java
public class WorkflowExecutionResponse {
    public String workflowId;
    public String status;  // "RUNNING", "COMPLETED", etc.

    // TODO: Add constructors, getters, setters
}
```

---

### 6. Application Configuration

**File:** `src/main/resources/application.yml`

**What it should contain:**
- Server port configuration
- Temporal connection settings (host, port, task-queue)
- OpenAI configuration (api-key, model, dummy-mode)
- Spring Actuator endpoints for health checks

**Example structure:**
```yaml
server:
  port: 8080

temporal:
  host: localhost
  port: 7233
  task-queue: pr-review

openai:
  api-key: ${OPENAI_API_KEY:dummy}
  model: gpt-4o-mini
  dummy-mode: ${DUMMY_MODE:true}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

**Why YAML?** Environment-specific configuration without code changes. In Exercise 904, you'll add cloud-specific config.

---

### 7. Maven Configuration

**File:** `pom.xml`

**What it should contain:**
- Spring Boot parent POM
- Spring Boot web starter (REST API)
- Spring Boot actuator (health checks)
- Temporal SDK dependencies (from 901)
- All other dependencies from 901 (OpenAI, JSON, etc.)

**Key additions to 901's pom.xml:**
```xml
<!-- Spring Boot Parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<!-- NEW: Spring Boot Dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Spring Boot Maven Plugin -->
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

---

## Testing Your Implementation

### Test Scenario 1: Clean PR (Should APPROVE)

**Terminal 1:** Start Temporal server
```bash
temporal server start-dev
```

**Terminal 2:** Start Spring Boot application
```bash
cd exercise-902-githubpr-springboot/java
mvn spring-boot:run
```

You should see output like:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

INFO: Temporal worker started successfully on task queue: pr-review
INFO: Tomcat started on port(s): 8080 (http)
```

**Terminal 3:** Submit clean PR
```bash
curl -X POST http://localhost:8080/api/review \
  -H "Content-Type: application/json" \
  -d '{
    "prTitle": "Add user profile feature",
    "prDescription": "Implements user profile viewing and editing with proper validation",
    "diff": "+class UserProfile {\n+  private String name;\n+  private String email;\n+  \n+  public void validateEmail() {\n+    if (!email.matches(\"^[A-Za-z0-9+_.-]+@(.+)$\")) {\n+      throw new ValidationException(\"Invalid email\");\n+    }\n+  }\n+}",
    "testSummary": {
      "passed": true,
      "totalTests": 15,
      "failedTests": 0,
      "durationMs": 200
    }
  }'
```

**Expected response (immediate - HTTP 202 Accepted):**
```json
{
  "workflowId": "pr-review-abc-123-xyz",
  "status": "RUNNING"
}
```

**Terminal 3:** Check result (wait 6-9 seconds, then query)
```bash
curl http://localhost:8080/api/review/pr-review-abc-123-xyz
```

**Expected result:**
```json
{
  "overallRecommendation": "APPROVE",
  "agentResults": [
    {
      "agentName": "Code Quality",
      "recommendation": "APPROVE",
      "riskLevel": "LOW",
      "findings": ["Clean code structure", "Proper validation"]
    },
    {
      "agentName": "Test Quality",
      "recommendation": "APPROVE",
      "riskLevel": "LOW",
      "findings": ["Good test coverage"]
    },
    {
      "agentName": "Security",
      "recommendation": "APPROVE",
      "riskLevel": "LOW",
      "findings": ["No security issues found"]
    }
  ],
  "metadata": {
    "tookMs": 6234,
    "model": "gpt-4o-mini"
  }
}
```

---

### Test Scenario 2: Security Issue (Should BLOCK)

**Terminal 3:** Submit PR with hardcoded secret
```bash
curl -X POST http://localhost:8080/api/review \
  -H "Content-Type: application/json" \
  -d '{
    "prTitle": "Add API integration",
    "prDescription": "Integrates with external API",
    "diff": "+class ApiClient {\n+  private static final String API_KEY = \"sk-live-abc123xyz789\";\n+  \n+  public void makeRequest() {\n+    httpClient.get(\"https://api.example.com/data\", API_KEY);\n+  }\n+}",
    "testSummary": {
      "passed": true,
      "totalTests": 5,
      "failedTests": 0,
      "durationMs": 100
    }
  }'
```

**Expected overall recommendation:** `"BLOCK"`

**Why?** Security agent detects hardcoded API key. Aggregation logic: any BLOCK → overall BLOCK.

---

### Test Scenario 3: Code Quality Issues (Should REQUEST_CHANGES)

**Terminal 3:** Submit PR with code quality issues
```bash
curl -X POST http://localhost:8080/api/review \
  -H "Content-Type: application/json" \
  -d '{
    "prTitle": "Add data processing",
    "prDescription": "Processes user data",
    "diff": "+void p(String d) {\n+  String[] a = d.split(\",\");\n+  for(int i=0;i<a.length;i++) {\n+    System.out.println(a[i]);\n+  }\n+}",
    "testSummary": {
      "passed": false,
      "totalTests": 2,
      "failedTests": 1,
      "durationMs": 50
    }
  }'
```

**Expected overall recommendation:** `"REQUEST_CHANGES"`

**Why?** Code quality agent flags poor naming, test quality agent flags failing tests. Aggregation logic: no BLOCK but has REQUEST_CHANGES → overall REQUEST_CHANGES.

---

## Verification Checklist

✅ Spring Boot application starts successfully
✅ Temporal worker registers and starts automatically
✅ POST /api/review returns 202 Accepted immediately (non-blocking)
✅ GET /api/review/{workflowId} returns results after workflow completes
✅ Test Scenario 1 (clean PR) → APPROVE
✅ Test Scenario 2 (security issue) → BLOCK
✅ Test Scenario 3 (code quality issues) → REQUEST_CHANGES
✅ Health check endpoint works: `curl http://localhost:8080/actuator/health`
✅ Temporal UI shows workflow executions: http://localhost:8233

---

## Key Differences from Exercise 901

| Aspect | Exercise 901 (Plain Java) | Exercise 902 (Spring Boot) |
|--------|--------------------------|----------------------------|
| **Entry Point** | `WorkerApp.main()` + `Starter.main()` | `Application.main()` (single process) |
| **Workflow Trigger** | Manual `main()` execution | REST API POST /api/review |
| **Dependency Injection** | Manual `new Agent()` | Spring `@Autowired` |
| **Configuration** | Hardcoded values | `application.yml` |
| **Worker Lifecycle** | Manual start in main() | Auto-start with `@PostConstruct` |
| **Health Checks** | None | `/actuator/health` |
| **Result Retrieval** | Print to console | REST API GET /api/review/{id} |
| **Production Ready** | No | Yes |

---

## Common Pitfalls

### 1. Blocking HTTP Thread

❌ **Wrong:**
```java
@PostMapping("/review")
public ReviewResponse submitReview(@RequestBody ReviewRequest request) {
    ReviewResponse result = workflow.review(request);  // Blocks 6-9 seconds!
    return result;
}
```

✅ **Correct:**
```java
@PostMapping("/review")
public WorkflowExecutionResponse submitReview(@RequestBody ReviewRequest request) {
    WorkflowClient.start(workflow::review, request);  // Returns immediately
    return new WorkflowExecutionResponse(workflowId, "RUNNING");
}
```

### 2. Forgetting @PostConstruct

❌ **Wrong:**
```java
@Component
public class TemporalWorker {
    public TemporalWorker() {
        factory.start();  // Runs too early, dependencies not injected yet!
    }
}
```

✅ **Correct:**
```java
@Component
public class TemporalWorker {
    @PostConstruct
    public void start() {
        factory.start();  // Runs after Spring injects all dependencies
    }
}
```

### 3. Not Registering Activities with Dependencies

❌ **Wrong:**
```java
worker.registerActivitiesImplementations(
    new CodeQualityActivityImpl()  // Missing agent dependency!
);
```

✅ **Correct:**
```java
worker.registerActivitiesImplementations(
    new CodeQualityActivityImpl(codeQualityAgent)  // Pass injected agent
);
```

### 4. Missing Spring Boot Parent POM

❌ **Wrong:** Copy 901's pom.xml exactly
✅ **Correct:** Add Spring Boot parent and dependencies

---

## Success Criteria

Your implementation is successful when:

1. ✅ **Single process** runs both worker and REST API
2. ✅ **POST /api/review** returns immediately (non-blocking)
3. ✅ **GET /api/review/{id}** retrieves results
4. ✅ **All 3 test scenarios** produce correct recommendations
5. ✅ **Configuration** lives in application.yml (no hardcoded values)
6. ✅ **Health check** endpoint responds
7. ✅ **Temporal UI** shows workflow executions
8. ✅ **No manual bean creation** (all via Spring DI)

---

## What's Next?

**Exercise 903:** Production Deployment (Kubernetes + Prometheus + Grafana)
**Exercise 904:** Temporal Cloud Migration (mTLS + Cloud UI)
**Exercise 905:** Batch Processing (100+ PRs with continue-as-new)

---

## Helpful Resources

- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Boot + Temporal Guide: https://docs.temporal.io/dev-guide/java/foundations#run-a-dev-worker
- Spring Dependency Injection: https://spring.io/guides/gs/spring-boot
- Temporal Java SDK: https://docs.temporal.io/dev-guide/java

---

## Tips

1. **Start small** - Get Application.java and TemporalConfig.java working first
2. **Copy from 901** - Reuse all workflow/activity/agent files
3. **Test incrementally** - Verify worker starts before adding REST API
4. **Use Temporal UI** - Check http://localhost:8233 to see if workflows execute
5. **Check logs** - Spring Boot provides excellent startup logs

Good luck! 🚀
