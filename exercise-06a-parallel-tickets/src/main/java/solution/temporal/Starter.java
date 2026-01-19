package solution.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import solution.domain.TriageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Starter {
    private static final String TASK_QUEUE = "support-triage";

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // ================================================================
        // 10 sample tickets for parallel processing demo
        // ================================================================
        String[] tickets = {
                "TKT-001|How do I reset my password? I forgot it.",
                "TKT-002|Account hacked! Someone charged my card 4532-1234-5678-9012 for $500!",
                "TKT-003|Can't login to my account, getting error 403",
                "TKT-004|Please update my email address to john@example.com",
                "TKT-005|Billing question about my last invoice",
                "TKT-006|URGENT: Production system is down!",
                "TKT-007|How do I export my data?",
                "TKT-008|Feature request: dark mode support",
                "TKT-009|My SSN 123-45-6789 was exposed in a data breach",
                "TKT-010|General feedback about the product"
        };

        System.out.println("Starting " + tickets.length + " workflows in PARALLEL...\n");
        long startTime = System.currentTimeMillis();

        // Store futures for parallel execution
        List<CompletableFuture<TriageResult>> futures = new ArrayList<>();
        List<String> workflowIds = new ArrayList<>();

        // ================================================================
        // PATTERN 1: Business Identifier Workflow IDs
        // ================================================================
        // Instead of: TASK_QUEUE + "-" + UUID.randomUUID()
        // Use:        "triage-" + ticketId (e.g., "triage-TKT-001")
        //
        // Benefits:
        // - Find workflows easily in Temporal UI by business entity
        // - Idempotent: same ticket ID = same workflow ID (prevents duplicates)
        // - Meaningful for logging, debugging, and operations
        // ================================================================

        // ================================================================
        // PATTERN 2: Parallel Workflow Execution
        // ================================================================
        // Instead of: blocking call in loop (workflow.triageTicket())
        // Use:        WorkflowClient.execute() returns CompletableFuture
        //
        // Benefits:
        // - All workflows start immediately (no waiting)
        // - Results collected when all complete
        // - Massive throughput improvement for batch processing
        // ================================================================

        // Start all workflows in parallel (non-blocking)
        for (String ticket : tickets) {
            String[] parts = ticket.split("\\|");
            String ticketId = parts[0];
            String ticketText = parts[1];

            // PATTERN 1: Business identifier workflow ID
            String workflowId = "triage-" + ticketId;  // e.g., "triage-TKT-001"
            workflowIds.add(workflowId);

            SupportTriageWorkflow workflow = client.newWorkflowStub(
                    SupportTriageWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .build()
            );

            // PATTERN 2: Start async - returns immediately with CompletableFuture
            CompletableFuture<TriageResult> future =
                    WorkflowClient.execute(workflow::triageTicket, ticketId, ticketText);
            futures.add(future);

            System.out.println("  Started: " + workflowId);
        }

        long startElapsed = System.currentTimeMillis() - startTime;
        System.out.println("\nAll " + tickets.length + " workflows started in " + startElapsed + "ms");
        System.out.println("Waiting for completion...\n");

        // Wait for all workflows to complete
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            allDone.join();  // Block until all complete
        } catch (Exception e) {
            System.err.println("Error waiting for workflows: " + e.getMessage());
        }

        // Collect results
        int successful = 0;
        int failed = 0;

        for (int i = 0; i < futures.size(); i++) {
            try {
                TriageResult result = futures.get(i).get();
                if (result.success) {
                    successful++;
                    System.out.println("  " + workflowIds.get(i) + " - SUCCESS");
                } else {
                    failed++;
                    System.out.println("  " + workflowIds.get(i) + " - FAILED: " + result.error);
                }
            } catch (InterruptedException | ExecutionException e) {
                failed++;
                System.err.println("  " + workflowIds.get(i) + " - ERROR: " + e.getMessage());
            }
        }

        long totalElapsed = System.currentTimeMillis() - startTime;

        // Summary
        String separator = "==================================================";
        System.out.println("\n" + separator);
        System.out.println("RESULTS (Parallel Execution)");
        System.out.println(separator);
        System.out.println("Tickets processed successfully: " + successful);
        System.out.println("Tickets failed: " + failed);
        System.out.println("Total time: " + totalElapsed + "ms");
        System.out.println("Average time per ticket: " + (totalElapsed / tickets.length) + "ms");
        System.out.println(separator);
        System.out.println("\nView workflows in Temporal UI: http://localhost:8233");
        System.out.println("Search by workflow ID: triage-TKT-001 through triage-TKT-010");
    }
}
