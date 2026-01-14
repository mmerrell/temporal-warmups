package solution.temporal;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import exercise.SupportTriageService;
import solution.domain.TicketClassification;

public class TicketClassifier {
    public TicketClassification classifyTicket(String scrubbedText) {
        System.out.println("\n[Classification Agent] Analyzing ticket...");
        String apiKey = System.getenv("OPENAI_API_KEY");
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4)
                    .addSystemMessage("You are a support ticket classifier. Analyze the ticket and classify it. " +
                            "Respond with only the classification in this format:\n" +
                            "Category: billing|technical|account|general\n" +
                            "Urgency: low|medium|high|critical\n" +
                            "Confidence: 0.0-1.0\n" +
                            "Reasoning: brief explanation")
                    .addUserMessage("Classify this support ticket: " + scrubbedText)
                    .temperature(0.0)
                    .maxCompletionTokens(300L)
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);
            String response = completion.choices().get(0).message().content().get();

            // Parse response (simple text parsing)
            TicketClassification classification = parseClassification(response);

            System.out.println("  Category: " + classification.category);
            System.out.println("  Urgency: " + classification.urgency);
            System.out.println("  Confidence: " + String.format("%.2f", classification.confidence));
            System.out.println("  ✓ Classification completed");

            return classification;

        } catch (Exception e) {
            System.out.println("  ✗ Classification FAILED: " + e.getMessage());
            throw new RuntimeException("Classification failed", e);
        }
    }
    // Helper method to parse classification response
    private TicketClassification parseClassification(String response) {
        String category = "general";
        String urgency = "medium";
        double confidence = 0.5;
        String reasoning = "";

        for (String line : response.split("\n")) {
            String trim = line.substring(line.indexOf(":") + 1).trim();
            if (line.toLowerCase().startsWith("category:")) {
                category = trim.toLowerCase();
            } else if (line.toLowerCase().startsWith("urgency:")) {
                urgency = trim.toLowerCase();
            } else if (line.toLowerCase().startsWith("confidence:")) {
                try {
                    confidence = Double.parseDouble(trim);
                } catch (NumberFormatException e) {
                    confidence = 0.5;
                }
            } else if (line.toLowerCase().startsWith("reasoning:")) {
                reasoning = trim;
            }
        }

        return new TicketClassification(category, urgency, confidence, reasoning);
    }
}
