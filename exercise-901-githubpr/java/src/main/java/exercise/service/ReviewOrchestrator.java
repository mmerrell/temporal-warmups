package exercise.service;

import exercise.agents.CodeQualityAgent;
import exercise.agents.SecurityAgent;
import exercise.agents.TestQualityAgent;
import exercise.model.*;

import java.time.Instant;
import java.util.Arrays;

/**
 * Review Orchestrator - coordinates the three AI agents to review a pull request.
 *
 * THIS IS INTENTIONALLY BAD ARCHITECTURE!
 *
 * Problems with this approach:
 * - No retries: If any agent fails, the entire request fails
 * - No durability: If the server crashes, all progress is lost
 * - No observability: Can't see what agents are doing or why they failed
 * - Unbounded latency: Blocks HTTP thread for 6-9+ seconds (3 agents × 2-3 seconds each)
 * - Cannot resume after failure: Must start over from scratch
 * - Unsafe for scale: Thread-per-request model doesn't scale beyond a single server
 *
 * This service exists to demonstrate these problems, which Temporal will solve.
 */
public class ReviewOrchestrator {

    private final CodeQualityAgent codeQualityAgent;
    private final TestQualityAgent testQualityAgent;
    private final SecurityAgent securityAgent;

    public ReviewOrchestrator(
        CodeQualityAgent codeQualityAgent,
        TestQualityAgent testQualityAgent,
        SecurityAgent securityAgent
    ) {
        this.codeQualityAgent = codeQualityAgent;
        this.testQualityAgent = testQualityAgent;
        this.securityAgent = securityAgent;
    }

    /**
     * Orchestrates the PR review by calling three agents sequentially.
     * This blocks the calling thread for the entire duration!
     */
    public ReviewResponse review(ReviewRequest request) {
        long startMs = System.currentTimeMillis();

        System.out.println("=".repeat(60));
        System.out.println("Starting PR Review: " + request.prTitle);
        System.out.println("=".repeat(60));

        // TODO: No retries - if any agent fails, the entire request fails
        // If the OpenAI API times out or returns a 500 error, we have no retry logic.
        // The entire request fails and the client gets a 500 error.

        // TODO: No durability - if the server crashes, all progress is lost
        // If we crash after the Code Quality agent completes, we lose that work.
        // When the server restarts, we have to start from scratch.

        // TODO: No observability - can't see what agents are doing or why they failed
        // There's no way to see intermediate results or debug agent failures.
        // We only get a final success or failure.

        // TODO: Unbounded latency - blocks HTTP thread for 6-9+ seconds
        // Each agent call takes 2-3 seconds. We call them sequentially.
        // This means the HTTP request thread is blocked for 6-9+ seconds.
        // Under load, this will exhaust the thread pool.

        // TODO: Cannot resume after failure
        // If the Security agent fails, we can't retry just that agent.
        // We have to re-run ALL agents, wasting time and money (LLM API costs).

        // TODO: Unsafe for scale - thread-per-request model doesn't scale
        // Every HTTP request blocks a thread for 6-9+ seconds.
        // With 100 concurrent requests, we'd need 100 threads all blocked waiting.
        // This doesn't scale beyond a single server with a large thread pool.

        try {
            // 1. Call Code Quality Agent (BLOCKS for ~2-3 seconds)
            System.out.println("[1/3] Calling Code Quality Agent...");
            AgentResult codeQuality = codeQualityAgent.analyze(
                request.prTitle,
                request.prDescription,
                request.diff
            );
            System.out.println("      → " + codeQuality.recommendation + " (Risk: " + codeQuality.riskLevel + ")");

            // 2. Call Test Quality Agent (BLOCKS for ~2-3 seconds)
            System.out.println("[2/3] Calling Test Quality Agent...");
            AgentResult testQuality = testQualityAgent.analyze(
                request.prTitle,
                request.prDescription,
                request.diff,
                request.testSummary
            );
            System.out.println("      → " + testQuality.recommendation + " (Risk: " + testQuality.riskLevel + ")");

            // 3. Call Security Agent (BLOCKS for ~2-3 seconds)
            System.out.println("[3/3] Calling Security Agent...");
            AgentResult security = securityAgent.analyze(
                request.prTitle,
                request.prDescription,
                request.diff
            );
            System.out.println("      → " + security.recommendation + " (Risk: " + security.riskLevel + ")");

            // 4. Aggregate results
            String overall = aggregate(codeQuality, testQuality, security);

            // 5. Build response
            long tookMs = System.currentTimeMillis() - startMs;

            Metadata metadata = new Metadata(
                Instant.now().toString(),
                tookMs,
                System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini")
            );

            ReviewResponse response = new ReviewResponse(
                overall,
                Arrays.asList(codeQuality, testQuality, security),
                metadata
            );

            System.out.println("=".repeat(60));
            System.out.println("Review Complete: " + overall + " (took " + tookMs + "ms)");
            System.out.println("=".repeat(60));

            return response;

        } catch (Exception e) {
            // Fail fast - no retry logic, no graceful degradation
            System.err.println("Review failed: " + e.getMessage());
            throw new RuntimeException("Review orchestration failed", e);
        }
    }

    /**
     * Aggregates agent results into an overall recommendation.
     *
     * Logic:
     * - If ANY agent returns BLOCK → overall is BLOCK
     * - Else if ANY agent returns REQUEST_CHANGES → overall is REQUEST_CHANGES
     * - Else → overall is APPROVE
     */
    private String aggregate(AgentResult... results) {
        // Check for BLOCK
        for (AgentResult result : results) {
            if ("BLOCK".equals(result.recommendation)) {
                return "BLOCK";
            }
        }

        // Check for REQUEST_CHANGES
        for (AgentResult result : results) {
            if ("REQUEST_CHANGES".equals(result.recommendation)) {
                return "REQUEST_CHANGES";
            }
        }

        // All agents approved
        return "APPROVE";
    }
}
