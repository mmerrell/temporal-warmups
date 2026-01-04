package solution.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import solution.domain.TicketClassification;
import solution.domain.TriageResult;
import solution.temporal.activity.PIIScrubberActivity;
import solution.temporal.activity.TicketClassifierActivity;

import java.time.Duration;

public class SupportTriageWorkflowImpl implements SupportTriageWorkflow {
    // 1. configure how activities should behave
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))  // How long can one attempt take?
            .setRetryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(2)) // How long before first retry? 2 sec is better for LLM
                .setBackoffCoefficient(2)   // Multiply wait time by what?
                .build())
            .build();

    //2. create activity stubs with non-existent interfaces. Using Workflow
    private final PIIScrubberActivity scrubber = Workflow.newActivityStub(
            PIIScrubberActivity.class, ACTIVITY_OPTIONS
    );
    private final TicketClassifierActivity classifier = Workflow.newActivityStub(
            TicketClassifierActivity.class, ACTIVITY_OPTIONS
    );
    // 3. call the workflow method
    @Override
    public TriageResult triageTicket(String ticketId, String ticketText) {
        Workflow.getLogger(SupportTriageWorkflowImpl.class)
                .info("Ticket triage started for ticketId: " + ticketId);
        String separator = "==================================================";

        try {
            // Step 1: Scrub PII
            String scrubbedText = scrubber.scrubPII(ticketText);

            // Step 2: Classify ticket
            TicketClassification classification = classifier.classifyTicket(scrubbedText);

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

    @Override
    public void approveTicket(boolean approved) {

    }
}
