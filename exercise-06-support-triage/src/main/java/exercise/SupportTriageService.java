package exercise;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * Pre-Temporal LLM-Powered Support Triage Service
 *
 * This demonstrates the problems with traditional AI orchestration:
 * - No retry logic for LLM API failures
 * - No durability if process crashes mid-analysis
 * - No visibility into multi-agent decision process
 * - Manual error handling and coordination between AI agents
 * - If one agent fails, must re-run entire workflow (wasteful)
 * - No audit trail of AI decisions
 * - No human-in-the-loop approval for high-risk cases
 */
public class SupportTriageService {
    private final OpenAIClient client;

    public SupportTriageService(String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .build();
    }

    /**
     * Step 1: PII Scrubbing Agent
     * Uses GPT-4 to identify and redact sensitive information
     */
    public String scrubPII(String ticketText) {
        System.out.println("\n[PII Scrubber Agent] Processing ticket...");

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

    /**
     * Step 2: Classification Agent
     * Uses GPT-4 to classify ticket urgency and category
     */
    public TicketClassification classifyTicket(String scrubbedText) {
        System.out.println("\n[Classification Agent] Analyzing ticket...");

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

    /**
     * Main triage orchestration - coordinates two AI agents
     */
    public TriageResult triageTicket(String ticketId, String ticketText) {
        String separator = "==================================================";
        System.out.println("\n" + separator);
        System.out.println("Processing Ticket: " + ticketId);
        System.out.println(separator);

        try {
            // Step 1: Scrub PII
            String scrubbedText = scrubPII(ticketText);

            // Step 2: Classify ticket
            TicketClassification classification = classifyTicket(scrubbedText);

            // Step 3: Route decision (deterministic logic)
            boolean needsHumanReview =
                classification.confidence < 0.7 ||
                classification.urgency.equals("critical");

            // Step 4: Create CRM case (simulated)
            String caseId = "CASE-" + System.currentTimeMillis();
            System.out.println("\n[CRM] Case created: " + caseId);
            if (needsHumanReview) {
                System.out.println("[CRM] ⚠️  Flagged for human review (high-risk/low-confidence)");
            }

            System.out.println("\n" + separator);
            System.out.println("✓ Ticket " + ticketId + " processed successfully");
            System.out.println(separator);

            return new TriageResult(true, ticketId, classification, caseId, null, needsHumanReview);

        } catch (Exception e) {
            System.out.println("\n" + separator);
            System.out.println("✗ Ticket " + ticketId + " FAILED: " + e.getMessage());
            System.out.println(separator);

            return new TriageResult(false, ticketId, null, null, e.getMessage(), false);
        }
    }

    // Helper method to parse classification response
    private TicketClassification parseClassification(String response) {
        String category = "general";
        String urgency = "medium";
        double confidence = 0.5;
        String reasoning = "";

        for (String line : response.split("\n")) {
            if (line.toLowerCase().startsWith("category:")) {
                category = line.substring(line.indexOf(":") + 1).trim().toLowerCase();
            } else if (line.toLowerCase().startsWith("urgency:")) {
                urgency = line.substring(line.indexOf(":") + 1).trim().toLowerCase();
            } else if (line.toLowerCase().startsWith("confidence:")) {
                try {
                    confidence = Double.parseDouble(line.substring(line.indexOf(":") + 1).trim());
                } catch (NumberFormatException e) {
                    confidence = 0.5;
                }
            } else if (line.toLowerCase().startsWith("reasoning:")) {
                reasoning = line.substring(line.indexOf(":") + 1).trim();
            }
        }

        return new TicketClassification(category, urgency, confidence, reasoning);
    }

    // Simple domain models

    public static class TicketClassification {
        public String category;
        public String urgency;
        public double confidence;
        public String reasoning;

        public TicketClassification() {}

        public TicketClassification(String category, String urgency, double confidence, String reasoning) {
            this.category = category;
            this.urgency = urgency;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
    }

    public static class TriageResult {
        public boolean success;
        public String ticketId;
        public TicketClassification classification;
        public String caseId;
        public String error;
        public boolean needsHumanReview;

        public TriageResult() {}

        public TriageResult(boolean success, String ticketId, TicketClassification classification,
                    String caseId, String error, boolean needsHumanReview) {
            this.success = success;
            this.ticketId = ticketId;
            this.classification = classification;
            this.caseId = caseId;
            this.error = error;
            this.needsHumanReview = needsHumanReview;
        }
    }

    public static void main(String[] args) {
        // Check for API key
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("ERROR: OPENAI_API_KEY environment variable not set");
            System.out.println("Please set it: export OPENAI_API_KEY=sk-...");
            System.exit(1);
        }

        SupportTriageService service = new SupportTriageService(apiKey);

        // Sample tickets
        String[] tickets = {
            "TKT-001|How do I reset my password? I forgot it.",
            "TKT-002|Account hacked! Someone charged my card 4532-1234-5678-9012 for $500!"
        };

        int successful = 0;
        int failed = 0;

        for (String ticket : tickets) {
            String[] parts = ticket.split("\\|");
            String ticketId = parts[0];
            String ticketText = parts[1];

            TriageResult result = service.triageTicket(ticketId, ticketText);

            if (result.success) {
                successful++;
            } else {
                failed++;
            }

            // Pause between tickets
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Summary
        String separator = "==================================================";
        System.out.println("\n\n" + separator);
        System.out.println("RESULTS");
        System.out.println(separator);
        System.out.println("Tickets processed successfully: " + successful);
        System.out.println("Tickets failed: " + failed);

        System.out.println("\n" + separator);
        System.out.println("PROBLEMS WITH THIS APPROACH");
        System.out.println(separator);
        System.out.println("1. No retry logic - LLM API failures cause complete workflow failure");
        System.out.println("2. Wasted API costs - must re-run BOTH agents if one fails");
        System.out.println("3. No durability - process crash loses all analysis progress");
        System.out.println("4. No visibility - can't track multi-agent decision flow");
        System.out.println("5. Manual orchestration - fragile coordination between AI agents");
        System.out.println("6. No audit trail - can't review AI decisions for compliance");
        System.out.println("7. No human-in-the-loop - can't pause for approval on high-risk tickets");
        System.out.println("\nTemporal solves all of these!");
        System.out.println("- Automatic retries for transient LLM API failures");
        System.out.println("- Only retry failed agent calls, not entire workflow");
        System.out.println("- Full audit trail of AI agent decisions in Temporal UI");
        System.out.println("- Durable execution survives crashes");
        System.out.println("- Signal pattern enables human-in-the-loop approval");
        System.out.println(separator + "\n");
    }
}
