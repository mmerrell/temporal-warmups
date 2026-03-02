package compliance;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import compliance.domain.CategoryRequest;
import compliance.domain.TransactionCategory;

/**
 * AI-powered transaction categorization agent using GPT-4.
 * Classifies transactions for regulatory reporting and analytics.
 *
 * [GIVEN] - Students use this class as-is. Focus on Temporal integration, not AI logic.
 */
public class TransactionCategorizerAgent {

    private final OpenAIClient client;

    public TransactionCategorizerAgent() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENAI_API_KEY environment variable is required");
        }
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public TransactionCategory categorize(CategoryRequest request) {
        String prompt = String.format(
            "You are a financial transaction categorization AI for regulatory reporting. " +
            "Categorize this transaction.\n\n" +
            "Transaction Details:\n" +
            "- Transaction ID: %s\n" +
            "- Amount: $%.2f\n" +
            "- Description: %s\n" +
            "- Sender Country: %s\n" +
            "- Receiver Country: %s\n\n" +
            "Respond in EXACTLY this format (no other text):\n" +
            "CATEGORY: [HOUSING|FOOD_BEVERAGE|INTERNATIONAL_TRANSFER|INVESTMENT|UTILITIES|ENTERTAINMENT|BUSINESS|OTHER]\n" +
            "SUB_CATEGORY: [specific description, e.g., 'Rent Payment', 'Coffee Purchase']\n" +
            "REGULATORY_FLAGS: [BSA_CTR|OFAC|FBAR|SAR|NONE]\n" +
            "REASONING: [one line explanation]\n\n" +
            "Regulatory flag rules:\n" +
            "- BSA_CTR: Cash transactions >$10,000\n" +
            "- OFAC: Sanctioned country involvement\n" +
            "- FBAR: Foreign account >$10,000\n" +
            "- SAR: Suspicious activity (structuring, unusual patterns)\n" +
            "- NONE: No regulatory flags",
            request.getTransactionId(),
            request.getAmount(),
            request.getDescription(),
            request.getSenderCountry(),
            request.getReceiverCountry()
        );

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .addUserMessage(prompt)
                .temperature(0.1)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);
        String response = completion.choices().get(0).message().content().orElse("");

        return parseCategoryResponse(request.getTransactionId(), response);
    }

    private TransactionCategory parseCategoryResponse(String transactionId, String response) {
        String category = "OTHER";
        String subCategory = "Unknown";
        String regulatoryFlags = "NONE";
        String reasoning = "Unable to parse AI response";

        try {
            for (String line : response.split("\n")) {
                line = line.trim();
                if (line.startsWith("CATEGORY:")) {
                    category = line.substring("CATEGORY:".length()).trim();
                } else if (line.startsWith("SUB_CATEGORY:")) {
                    subCategory = line.substring("SUB_CATEGORY:".length()).trim();
                } else if (line.startsWith("REGULATORY_FLAGS:")) {
                    regulatoryFlags = line.substring("REGULATORY_FLAGS:".length()).trim();
                } else if (line.startsWith("REASONING:")) {
                    reasoning = line.substring("REASONING:".length()).trim();
                }
            }
        } catch (Exception e) {
            reasoning = "Parse error: " + e.getMessage() + " | Raw: " + response;
        }

        return new TransactionCategory(transactionId, category, subCategory,
                regulatoryFlags, reasoning);
    }
}
