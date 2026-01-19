package exercise.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import exercise.llm.LlmClient;
import exercise.llm.LlmOptions;
import exercise.llm.Message;
import exercise.llm.OpenAiLlmClient;
import exercise.model.AgentResult;
import exercise.model.TestSummary;

import java.util.Arrays;
import java.util.List;

/**
 * Test Quality Agent - assesses whether the PR is adequately tested.
 * Applies strict rules about test coverage and suggests high-value tests.
 */
public class TestQualityAgent {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public TestQualityAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
    }

    public TestQualityAgent() {
        this.llmClient = new OpenAiLlmClient();
        this.objectMapper = new ObjectMapper();
    }

    public AgentResult analyze(String prTitle, String prDescription, String diff, TestSummary testSummary) {
        try {
            // If testSummary is null, create a default one
            if (testSummary == null) {
                testSummary = new TestSummary(true, 0, 0, 0);
            }

            // Rule 1: If tests failed, BLOCK immediately
            if (!testSummary.passed) {
                return new AgentResult(
                    "Test Quality",
                    "HIGH",
                    "BLOCK",
                    Arrays.asList(
                        "Tests are failing - " + testSummary.failedTests + " out of " + testSummary.totalTests + " tests failed",
                        "All tests must pass before the PR can be approved",
                        "Fix the failing tests and ensure the build is green"
                    )
                );
            }

            // Build system prompt with rules
            String systemPrompt = buildSystemPrompt(testSummary);

            // Build user prompt with PR details
            String userPrompt = String.format(
                "PR Title: %s\n\nPR Description: %s\n\nDiff:\n%s\n\nTest Summary:\n- Passed: %s\n- Total Tests: %d\n- Failed Tests: %d\n- Duration: %d ms",
                prTitle,
                prDescription,
                diff,
                testSummary.passed,
                testSummary.totalTests,
                testSummary.failedTests,
                testSummary.durationMs
            );

            // Call LLM
            List<Message> messages = Arrays.asList(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
            );

            LlmOptions options = new LlmOptions(
                System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini"),
                0.2,
                "json_object"
            );

            String response = llmClient.chat(messages, options);

            // Parse JSON response into AgentResult
            return objectMapper.readValue(response, AgentResult.class);

        } catch (Exception e) {
            // Fail fast - no retry logic!
            throw new RuntimeException("Test Quality Agent failed: " + e.getMessage(), e);
        }
    }

    private String buildSystemPrompt(TestSummary testSummary) {
        return "You are a Test Quality Reviewer analyzing pull request diffs.\n\n" +
               "Your task is to assess whether the diff is adequately tested.\n\n" +
               "STRICT RULES:\n" +
               "1. If tests FAILED (testSummary.passed == false) -> BLOCK\n" +
               "2. If diff introduces new logic, branching, validation, or API behavior WITHOUT tests -> REQUEST_CHANGES\n" +
               "3. If diff appears to be refactor/comments/docs only -> APPROVE\n" +
               "4. If changes affect auth, validation, or error handling WITHOUT tests -> REQUEST_CHANGES\n\n" +
               "Risk Level Guidelines:\n" +
               "- LOW: Changes are well-tested or don't require new tests\n" +
               "- MEDIUM: Some new logic without tests, but not critical\n" +
               "- HIGH: Critical logic (auth, validation, error handling) without tests, or tests failing\n\n" +
               "IMPORTANT:\n" +
               "- Your findings MUST include exactly 3 specific, high-value test suggestions\n" +
               "- Test suggestions should focus on:\n" +
               "  * Edge cases and boundary conditions\n" +
               "  * Error handling and failure scenarios\n" +
               "  * Integration points and data flow\n" +
               "- Be specific about what to test and why\n\n" +
               "Respond ONLY with valid JSON matching this exact structure:\n" +
               "{\n" +
               "  \"agentName\": \"Test Quality\",\n" +
               "  \"riskLevel\": \"LOW|MEDIUM|HIGH\",\n" +
               "  \"recommendation\": \"APPROVE|REQUEST_CHANGES|BLOCK\",\n" +
               "  \"findings\": [\n" +
               "    \"Clear assessment of test coverage\",\n" +
               "    \"Test Suggestion 1: Specific test case with rationale\",\n" +
               "    \"Test Suggestion 2: Specific test case with rationale\",\n" +
               "    \"Test Suggestion 3: Specific test case with rationale\"\n" +
               "  ]\n" +
               "}";
    }
}
