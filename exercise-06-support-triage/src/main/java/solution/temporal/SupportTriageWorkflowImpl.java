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
    boolean approvalReceived = false;  // Has signal arrived?
    boolean approved = false;           // What was the decision?

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

            // Step 3: Human in the loop
            Workflow.getLogger(SupportTriageWorkflowImpl.class)
                    .info("Ticket needs human review. Waiting for approval signal...");

            // Wait for signal with timeout
            boolean signalReceived = Workflow.await(
                    Duration.ofHours(24),           // Timeout after 24 hours
                    () -> approvalReceived          // Condition to check
            );

            if (!signalReceived) {
                // Timeout - no human responded
                return new TriageResult(false, ticketId, null, null,
                        "Timeout: No approval received within 24 hours", false);
            }

            if (!approved) {
                // Human rejected the ticket
                return new TriageResult(false, ticketId, null, null,
                        "Rejected by human reviewer", false);
            }

            Workflow.getLogger(SupportTriageWorkflowImpl.class)
                    .info("Ticket approved by human reviewer");

            // Step 4: Create CRM case (simulated)
            String caseId = "CASE-" + System.currentTimeMillis();
            Workflow.getLogger(SupportTriageWorkflowImpl.class).info("[CRM] Case created: " + caseId);

            System.out.println("\n" + separator);
            System.out.println("✓ Ticket " + ticketId + " processed successfully");
            System.out.println(separator);

            return new TriageResult(true, ticketId, classification, caseId, null, true);

        } catch (Exception e) {
            System.out.println("\n" + separator);
            System.out.println("✗ Ticket " + ticketId + " FAILED: " + e.getMessage());
            System.out.println(separator);

            return new TriageResult(false, ticketId, null, null, e.getMessage(), false);
        }
    }

    @Override
    public void approveTicket(boolean approved) {
        this.approved = approved;           // Store the decision
        this.approvalReceived = true;       // Mark that signal arrived

        Workflow.getLogger(SupportTriageWorkflowImpl.class)
                .info("Approval signal received: " + approved);
    }
}
