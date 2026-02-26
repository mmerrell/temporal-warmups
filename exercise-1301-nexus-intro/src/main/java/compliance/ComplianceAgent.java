package compliance;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import compliance.domain.ComplianceRequest;
import compliance.domain.ComplianceResult;

/**
 * [GIVEN] AI-powered compliance agent using GPT-4o-mini.
 *
 * This class calls OpenAI to assess whether a transaction should be approved.
 * Students use it as-is — focus is on wiring it into Temporal Nexus, not the AI logic.
 *
 * Requires: OPENAI_API_KEY environment variable
 */
public class ComplianceAgent {

    private final OpenAIClient client;

    public ComplianceAgent() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENAI_API_KEY environment variable is required");
        }
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public ComplianceResult checkCompliance(ComplianceRequest request) {
        String prompt = String.format(
            "You are a financial compliance AI agent at a bank. " +
            "Assess whether this payment transaction should be approved based on risk and regulations.\n\n" +
            "Transaction Details:\n" +
            "- Transaction ID: %s\n" +
            "- Amount: $%.2f\n" +
            "- Sender Country: %s\n" +
            "- Receiver Country: %s\n" +
            "- Description: %s\n\n" +
            "Respond in EXACTLY this format (no other text):\n" +
            "APPROVED: [true|false]\n" +
            "RISK_LEVEL: [LOW|MEDIUM|HIGH]\n" +
            "EXPLANATION: [one concise sentence]\n\n" +
            "Rules:\n" +
            "- Block (false) if receiver country is OFAC sanctioned: North Korea, Iran, Cuba, Syria, Venezuela\n" +
            "- HIGH risk if amount > $50,000 or sanctioned country route\n" +
            "- MEDIUM risk if amount > $10,000 or international transfer to unusual jurisdiction\n" +
            "- LOW risk for routine domestic/well-known international transfers\n" +
            "- HIGH risk always means APPROVED: false\n" +
            "- MEDIUM risk means APPROVED: true (note the risk but allow)\n" +
            "- LOW risk means APPROVED: true",
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

        return parseResponse(request.getTransactionId(), response);
    }

    private ComplianceResult parseResponse(String transactionId, String response) {
        boolean approved = false;
        String riskLevel = "HIGH";
        String explanation = "Unable to parse compliance response";

        try {
            for (String line : response.split("\n")) {
                line = line.trim();
                if (line.startsWith("APPROVED:")) {
                    approved = Boolean.parseBoolean(line.substring("APPROVED:".length()).trim());
                } else if (line.startsWith("RISK_LEVEL:")) {
                    riskLevel = line.substring("RISK_LEVEL:".length()).trim();
                } else if (line.startsWith("EXPLANATION:")) {
                    explanation = line.substring("EXPLANATION:".length()).trim();
                }
            }
        } catch (Exception e) {
            explanation = "Parse error: " + e.getMessage() + " | Raw: " + response;
        }

        return new ComplianceResult(transactionId, approved, riskLevel, explanation);
    }
}
