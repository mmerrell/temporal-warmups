package compliance.temporal;

import compliance.ComplianceAgent;
import compliance.domain.ComplianceRequest;
import compliance.domain.ComplianceResult;
import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.nexus.WorkflowClientOperationHandlers;
import shared.nexus.ComplianceNexusService;

/**
 * YOUR TURN: Implement the Nexus service handler.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  THIS IS THE KEY FILE IN THIS EXERCISE
 * ═══════════════════════════════════════════════════════════════════
 *
 * This class tells Temporal HOW to handle the checkCompliance Nexus operation.
 *
 * METAPHOR: Think of it as a controller in a REST API:
 *   - ComplianceNexusService (interface) = the API route definition
 *   - This @ServiceImpl class            = the controller that handles requests
 *
 * ── SYNC Handler ─────────────────────────────────────────────────
 *
 *   Sync means: "run this inline, return the result immediately."
 *   No Temporal workflow is started — the operation completes in-place.
 *   The Payments team's workflow blocks briefly while this runs.
 *
 *   Use WorkflowClientOperationHandlers.sync():
 *
 *     return WorkflowClientOperationHandlers.sync(
 *         (context, details, client, input) -> {
 *             // 'input' is the ComplianceRequest from the Payments team
 *             return agent.checkCompliance(input);
 *         });
 *
 *   The lambda parameters:
 *     context  — Nexus operation context (headers, metadata)
 *     details  — operation details (name, etc.)
 *     client   — a WorkflowClient (not needed for sync operations)
 *     input    — the ComplianceRequest sent by the Payments team
 *
 * ── ANNOTATIONS ──────────────────────────────────────────────────
 *
 *   @ServiceImpl(service = ComplianceNexusService.class)  ← on the class
 *   @OperationImpl                                         ← on each handler method
 *
 *   IMPORTANT: The handler method name MUST match the interface method name.
 *   Interface has: ComplianceResult checkCompliance(ComplianceRequest)
 *   Handler must be: public OperationHandler<ComplianceRequest, ComplianceResult> checkCompliance()
 *
 * ── What to implement ────────────────────────────────────────────
 *
 *   1. Add @ServiceImpl(service = ComplianceNexusService.class) to the class
 *   2. Store ComplianceAgent as a field (passed via constructor)
 *   3. Add @OperationImpl to the checkCompliance() method
 *   4. Return WorkflowClientOperationHandlers.sync(...) that calls agent.checkCompliance(input)
 *
 * ── In Exercise 1300, you'll also implement ──────────────────────
 *   - An ASYNC handler using WorkflowClientOperationHandlers.fromWorkflowMethod()
 *   - That starts a FraudDetectionWorkflow on the Compliance side
 *   - The caller gets a NexusOperationHandle to track long-running work
 */
// TODO: Add @ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {

    // TODO: Add a ComplianceAgent field
    // private final ComplianceAgent agent;

    // TODO: Add a constructor that accepts ComplianceAgent
    // public ComplianceNexusServiceImpl(ComplianceAgent agent) {
    //     this.agent = agent;
    // }

    // TODO: Add @OperationImpl annotation
    // Method name MUST match the interface: checkCompliance
    public OperationHandler<ComplianceRequest, ComplianceResult> checkCompliance() {
        // TODO: Return WorkflowClientOperationHandlers.sync(...)
        //   Inside the lambda: return agent.checkCompliance(input)
        throw new UnsupportedOperationException("TODO: implement checkCompliance handler");
    }
}
