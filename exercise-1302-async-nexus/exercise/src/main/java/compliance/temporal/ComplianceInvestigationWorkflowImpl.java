package compliance.temporal;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import compliance.temporal.activity.ComplianceInvestigationActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  TODO FILE 3 of 7: ComplianceInvestigationWorkflowImpl — Workflow Pattern (10 min)
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: Wire the 3-phase investigation activity inside a workflow.
 * You're given the interface (ComplianceInvestigationWorkflow); you implement the body.
 *
 * WHAT TO IMPLEMENT:
 *
 *   1. Define ACTIVITY_OPTIONS as a static final ActivityOptions:
 *      - startToCloseTimeout: Duration.ofMinutes(2)
 *        (TXN-GAMMA investigation takes ~60s — give it room)
 *      - retryPolicy: maxAttempts(3), initialInterval(1s), backoffCoefficient(2)
 *      - heartbeatTimeout: Duration.ofSeconds(10)
 *        (ComplianceInvestigator calls heartbeat — set a timeout shorter than each phase)
 *
 *   2. Create a ComplianceInvestigationActivity stub:
 *      - Use Workflow.newActivityStub(ComplianceInvestigationActivity.class, ACTIVITY_OPTIONS)
 *
 *   3. Implement investigate():
 *      - Call activity.investigate(request)
 *      - Return the result
 *
 * WHY HEARTBEAT TIMEOUT?
 *   ComplianceInvestigator calls Activity.getExecutionContext().heartbeat() in each phase.
 *   If the activity stops heartbeating (worker crash), Temporal detects it via heartbeatTimeout
 *   and can reschedule the activity on another worker.
 *   This is what makes "The Heist Test" (Checkpoint 4) work.
 *
 * LOGGING REMINDER:
 *   Use Workflow.getLogger(), never System.out.println() in workflow code.
 *   Workflows can replay — println fires on every replay. getLogger() does not.
 */
public class ComplianceInvestigationWorkflowImpl implements ComplianceInvestigationWorkflow {

    // TODO 1: Define ACTIVITY_OPTIONS
    // Hint: startToCloseTimeout=2min, heartbeatTimeout=10s, retryOptions with maxAttempts=3

    // TODO 2: Create activity stub using Workflow.newActivityStub(...)

    @Override
    public InvestigationResult investigate(InvestigationRequest request) {
        // TODO 3: call activity.investigate(request) and return the result
        return null;
    }
}
