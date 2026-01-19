package exercise.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import exercise.llm.LlmClient;
import exercise.llm.LlmOptions;
import exercise.llm.Message;
import exercise.llm.OpenAiLlmClient;
import exercise.model.AgentResult;

import java.util.Arrays;
import java.util.List;

/**
 * Security Agent - identifies practical application security issues.
 * Focuses on secrets, authentication/authorization, and injection vulnerabilities.
 */
public class SecurityAgent {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public SecurityAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
    }

    public SecurityAgent() {
        this.llmClient = new OpenAiLlmClient();
        this.objectMapper = new ObjectMapper();
    }

    public AgentResult analyze(String prTitle, String prDescription, String diff) {
        try {
            // Build system prompt with security rules
            String systemPrompt = buildSystemPrompt();

            // Build user prompt with PR details
            String userPrompt = String.format(
                "PR Title: %s\n\nPR Description: %s\n\nDiff:\n%s",
                prTitle,
                prDescription,
                diff
            );

            // Call LLM
            List<Message> messages = Arrays.asList(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
            );

            LlmOptions options = new LlmOptions(
                System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini"),
                0.1,  // Very low temperature for consistent security analysis
                "json_object"
            );

            String response = llmClient.chat(messages, options);

            // Parse JSON response into AgentResult
            return objectMapper.readValue(response, AgentResult.class);

        } catch (Exception e) {
            // Fail fast - no retry logic!
            throw new RuntimeException("Security Agent failed: " + e.getMessage(), e);
        }
    }

    private String buildSystemPrompt() {
        return "You are a Security Reviewer analyzing pull request diffs for practical application security issues.\n\n" +
               "Focus on these THREE areas:\n\n" +
               "1. **Secrets & Sensitive Data**\n" +
               "   - Detect logging, returning, or storing secrets/tokens/passwords\n" +
               "   - Look for hardcoded API keys, database credentials, JWT secrets\n" +
               "   - Risk: HIGH if secrets appear directly in code\n" +
               "   - Examples: console.log(apiKey), commit messages with tokens, .env files committed\n\n" +
               "2. **Authentication/Authorization Correctness**\n" +
               "   - Detect changes that weaken or bypass authorization\n" +
               "   - Look for removed auth checks, inconsistent access control\n" +
               "   - Risk: HIGH -> BLOCK if auth bypass is possible\n" +
               "   - Examples: removing isAuthenticated() checks, allowing admin actions without verification\n\n" +
               "3. **Injection & Unsafe Input Handling**\n" +
               "   - Detect SQL injection, command injection, SSRF, path traversal, unsafe deserialization\n" +
               "   - Look for user input used directly in queries, system commands, file paths\n" +
               "   - Risk: HIGH -> BLOCK if dangerous sinks lack validation\n" +
               "   - Examples: string concatenation in SQL, eval() with user input, unvalidated file paths\n\n" +
               "Recommendation Logic:\n" +
               "- Auth bypass or injection risk -> BLOCK\n" +
               "- Questionable secrets handling -> REQUEST_CHANGES\n" +
               "- Otherwise -> APPROVE\n\n" +
               "Risk Level Guidelines:\n" +
               "- LOW: No security issues detected\n" +
               "- MEDIUM: Minor security concerns that should be addressed\n" +
               "- HIGH: Critical security vulnerabilities that must be fixed\n\n" +
               "IMPORTANT:\n" +
               "- Be specific: quote variable names, line numbers, function names\n" +
               "- Explain the attack vector and potential impact\n" +
               "- If no issues found, explicitly state \"No security issues detected\"\n\n" +
               "Respond ONLY with valid JSON matching this exact structure:\n" +
               "{\n" +
               "  \"agentName\": \"Security\",\n" +
               "  \"riskLevel\": \"LOW|MEDIUM|HIGH\",\n" +
               "  \"recommendation\": \"APPROVE|REQUEST_CHANGES|BLOCK\",\n" +
               "  \"findings\": [\"specific finding 1\", \"specific finding 2\", ...]\n" +
               "}";
    }
}
