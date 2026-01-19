package exercise;

import exercise.agents.CodeQualityAgent;
import exercise.agents.SecurityAgent;
import exercise.agents.TestQualityAgent;
import exercise.llm.LlmClient;
import exercise.llm.OpenAiLlmClient;
import exercise.service.ReviewOrchestrator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot application for GitHub PR AI Review System.
 *
 * This is the PRE-TEMPORAL baseline implementation.
 * It demonstrates a brittle, synchronous REST API that blocks for 6-9+ seconds
 * per request with no retries, no durability, and no observability.
 *
 * To run:
 *   mvn spring-boot:run
 *
 * Environment variables:
 *   - OPENAI_API_KEY: Required (unless DUMMY_MODE=true)
 *   - OPENAI_MODEL: Optional (defaults to gpt-4o-mini)
 *   - OPENAI_BASE_URL: Optional (defaults to OpenAI endpoint)
 *   - DUMMY_MODE: Set to "true" to use canned responses (no API costs)
 *
 * Test with:
 *   curl -X POST http://localhost:8080/review \
 *     -H "Content-Type: application/json" \
 *     -d '{
 *       "prTitle": "Add user authentication",
 *       "prDescription": "Implements JWT-based authentication",
 *       "diff": "... your diff here ...",
 *       "testSummary": {"passed": true, "totalTests": 5, "failedTests": 0, "durationMs": 120}
 *     }'
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("GitHub PR AI Review System - Pre-Temporal Baseline");
        System.out.println("=".repeat(60));
        System.out.println();

        // Check for DUMMY_MODE
        String dummyMode = System.getenv().getOrDefault("DUMMY_MODE", "false");
        if ("true".equalsIgnoreCase(dummyMode)) {
            System.out.println("⚠️  DUMMY_MODE is enabled - using canned responses");
            System.out.println("   (Set DUMMY_MODE=false to use real OpenAI API)");
        } else {
            String apiKey = System.getenv().getOrDefault("OPENAI_API_KEY", "");
            if (apiKey.isEmpty()) {
                System.err.println("❌ ERROR: OPENAI_API_KEY environment variable is required");
                System.err.println("   Set it with: export OPENAI_API_KEY=sk-your-key-here");
                System.err.println("   Or enable DUMMY_MODE: export DUMMY_MODE=true");
                System.exit(1);
            }
            String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");
            System.out.println("✓ Using OpenAI API with model: " + model);
        }

        System.out.println();
        System.out.println("Starting Spring Boot server on http://localhost:8080");
        System.out.println("POST /review to analyze pull requests");
        System.out.println("GET /health for health check");
        System.out.println("=".repeat(60));
        System.out.println();

        SpringApplication.run(Application.class, args);
    }

    /**
     * Bean: LLM client for making API calls to OpenAI.
     */
    @Bean
    public LlmClient llmClient() {
        return new OpenAiLlmClient();
    }

    /**
     * Bean: Code Quality Agent - evaluates code against quality criteria.
     */
    @Bean
    public CodeQualityAgent codeQualityAgent(LlmClient llmClient) {
        return new CodeQualityAgent(llmClient);
    }

    /**
     * Bean: Test Quality Agent - assesses test coverage and suggests tests.
     */
    @Bean
    public TestQualityAgent testQualityAgent(LlmClient llmClient) {
        return new TestQualityAgent(llmClient);
    }

    /**
     * Bean: Security Agent - identifies security vulnerabilities.
     */
    @Bean
    public SecurityAgent securityAgent(LlmClient llmClient) {
        return new SecurityAgent(llmClient);
    }

    /**
     * Bean: Review Orchestrator - coordinates the three agents.
     */
    @Bean
    public ReviewOrchestrator reviewOrchestrator(
        CodeQualityAgent codeQualityAgent,
        TestQualityAgent testQualityAgent,
        SecurityAgent securityAgent
    ) {
        return new ReviewOrchestrator(codeQualityAgent, testQualityAgent, securityAgent);
    }
}
