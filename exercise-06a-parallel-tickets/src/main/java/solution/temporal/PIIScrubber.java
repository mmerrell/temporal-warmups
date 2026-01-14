package solution.temporal;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class PIIScrubber {

    public String scrubPII(String ticketText) {
        System.out.println("\n[PII Scrubber Agent] Processing ticket...");
        String apiKey = System.getenv("OPENAI_API_KEY");
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4)
                    .addSystemMessage("You are a PII scrubbing assistant. Identify and redact all personally " +
                            "identifiable information including: SSN, credit card numbers, email addresses, " +
                            "phone numbers, physical addresses. Replace with [REDACTED_TYPE]. " +
                            "Return only the scrubbed text, nothing else.")
                    .addUserMessage("Scrub PII from this support ticket: " + ticketText)
                    .temperature(0.0)
                    .maxCompletionTokens(500L)
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);
            String scrubbedText = completion.choices().get(0).message().content().get();

            System.out.println("  ✓ PII scrubbing completed");
            return scrubbedText.trim();

        } catch (Exception e) {
            System.out.println("  ✗ PII scrubbing FAILED: " + e.getMessage());
            throw new RuntimeException("PII scrubbing failed", e);
        }
    }
}
