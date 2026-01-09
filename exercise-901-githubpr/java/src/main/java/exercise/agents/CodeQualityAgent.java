package exercise.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import exercise.llm.LlmClient;
import exercise.llm.LlmOptions;
import exercise.llm.Message;
import exercise.model.AgentResult;

import java.util.Arrays;
import java.util.List;

/**
 * Code Quality Agent - evaluates code quality against predefined criteria.
 * Uses LLM to analyze diff and provide specific, actionable feedback.
 */
public class CodeQualityAgent {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public CodeQualityAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
    }

    public AgentResult analyze(String prTitle, String prDescription, String diff) {
        try {
            // Build system prompt with criteria
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
                0.2,  // Low temperature for consistent, focused analysis
                "json_object"
            );

            String response = llmClient.chat(messages, options);

            // Parse JSON response into AgentResult
            return objectMapper.readValue(response, AgentResult.class);

        } catch (Exception e) {
            // Fail fast - no retry logic!
            throw new RuntimeException("Code Quality Agent failed: " + e.getMessage(), e);
        }
    }

    private String buildSystemPrompt() {
        return "You are a Code Quality Reviewer analyzing pull request diffs.\n\n" +
               "Your task is to evaluate the code against these criteria:\n" +
               "1. **Naming**: Are variables, functions, and classes clearly named?\n" +
               "2. **Function Size**: Are functions reasonably sized and focused?\n" +
               "3. **Responsibilities**: Does each component have a single, clear responsibility?\n" +
               "4. **Boundaries**: Are module boundaries and abstractions clear?\n" +
               "5. **Error Handling**: Is error handling present and appropriate?\n" +
               "6. **Refactoring Opportunities**: Are there obvious improvements?\n\n" +
               "CRITICAL REQUIREMENTS:\n" +
               "- Your findings MUST reference concrete issues found in the diff\n" +
               "- NO generic fluff like \"code looks good\" or \"nice work\"\n" +
               "- Be specific: quote variable names, line numbers, function names\n" +
               "- If you can't find specific issues, say so explicitly\n\n" +
               "Risk Level Guidelines:\n" +
               "- LOW: Minor style issues, suggestions for improvement\n" +
               "- MEDIUM: Unclear naming, functions that could be refactored\n" +
               "- HIGH: Missing error handling, unclear responsibilities, poor abstractions\n\n" +
               "Recommendation Guidelines:\n" +
               "- APPROVE: Code is good or has only minor style issues\n" +
               "- REQUEST_CHANGES: Code has clarity, maintainability, or structural issues\n" +
               "- BLOCK: Code has critical design flaws or missing error handling\n\n" +
               "Respond ONLY with valid JSON matching this exact structure:\n" +
               "{\n" +
               "  \"agentName\": \"Code Quality\",\n" +
               "  \"riskLevel\": \"LOW|MEDIUM|HIGH\",\n" +
               "  \"recommendation\": \"APPROVE|REQUEST_CHANGES|BLOCK\",\n" +
               "  \"findings\": [\"specific finding 1\", \"specific finding 2\", ...]\n" +
               "}";
    }
}
