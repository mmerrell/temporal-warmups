package compliance;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;

/**
 * AI-powered fraud detection agent using GPT-4.
 * Analyzes transaction patterns for suspicious activity.
 *
 * [GIVEN] - Students use this class as-is. Focus on Temporal integration, not AI logic.
 */
public class FraudDetectionAgent {

    private final OpenAIClient client;

    public FraudDetectionAgent() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENAI_API_KEY environment variable is required");
        }
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public RiskScreeningResult screenTransaction(RiskScreeningRequest request) {
        String prompt = String.format(
            "You are a financial fraud detection AI agent at a major bank. " +
            "Analyze this transaction for fraud risk and regulatory concerns.\n\n" +
            "Transaction Details:\n" +
            "- Transaction ID: %s\n" +
            "- Amount: $%.2f\n" +
            "- Sender Country: %s\n" +
            "- Receiver Country: %s\n" +
            "- Description: %s\n\n" +
            "Respond in EXACTLY this format (no other text):\n" +
            "RISK_LEVEL: [LOW|MEDIUM|HIGH|CRITICAL]\n" +
            "RISK_SCORE: [0.0-1.0]\n" +
            "REQUIRES_APPROVAL: [true|false]\n" +
            "SANCTIONS_FLAG: [true|false]\n" +
            "REASONING: [one line explanation]\n\n" +
            "Consider:\n" +
            "- Amounts near $10,000 (BSA/CTR structuring threshold)\n" +
            "- OFAC sanctioned countries (Russia, North Korea, Iran, Cuba, Syria)\n" +
            "- Tax haven destinations (Cayman Islands, Bermuda, BVI)\n" +
            "- Unusual patterns or amounts\n" +
            "- Large international transfers (>$50,000)",
            request.getTransactionId(),
            request.getAmount(),
            request.getSenderCountry(),
            request.getReceiverCountry(),
            request.getDescription()
        );

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .addUserMessage(prompt)
                .temperature(0.1)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);
        String response = completion.choices().get(0).message().content().orElse("");

        return parseRiskResponse(request.getTransactionId(), response);
    }

    private RiskScreeningResult parseRiskResponse(String transactionId, String response) {
        String riskLevel = "MEDIUM";
        double riskScore = 0.5;
        boolean requiresApproval = true;
        boolean flaggedSanctions = false;
        String reasoning = "Unable to parse AI response";

        try {
            for (String line : response.split("\n")) {
                line = line.trim();
                if (line.startsWith("RISK_LEVEL:")) {
                    riskLevel = line.substring("RISK_LEVEL:".length()).trim();
                } else if (line.startsWith("RISK_SCORE:")) {
                    riskScore = Double.parseDouble(line.substring("RISK_SCORE:".length()).trim());
                } else if (line.startsWith("REQUIRES_APPROVAL:")) {
                    requiresApproval = Boolean.parseBoolean(line.substring("REQUIRES_APPROVAL:".length()).trim());
                } else if (line.startsWith("SANCTIONS_FLAG:")) {
                    flaggedSanctions = Boolean.parseBoolean(line.substring("SANCTIONS_FLAG:".length()).trim());
                } else if (line.startsWith("REASONING:")) {
                    reasoning = line.substring("REASONING:".length()).trim();
                }
            }
        } catch (Exception e) {
            reasoning = "Parse error: " + e.getMessage() + " | Raw: " + response;
        }

        return new RiskScreeningResult(transactionId, riskLevel, riskScore,
                reasoning, requiresApproval, flaggedSanctions);
    }
}
