package shared.nexus;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

/**
 * [GIVEN] Nexus Service Interface — the shared contract between Payments and Compliance teams.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  THE SAME INTERFACE PATTERN AS EXERCISE 1301 — ONE KEY DIFFERENCE
 * ═══════════════════════════════════════════════════════════════════
 *
 * In Exercise 1301, this interface defined a SYNC operation (checkCompliance).
 * The handler ran inline and returned immediately.
 *
 * In Exercise 1302, the operation is identical from the CALLER's perspective —
 * you still call complianceService.investigate(request) in the workflow.
 *
 * The difference is ENTIRELY in the handler (ComplianceNexusServiceImpl):
 *   1301 handler: WorkflowClientOperationHandlers.sync(...)       ← returns value inline
 *   1302 handler: WorkflowClientOperationHandlers.fromWorkflowMethod(...) ← starts a workflow
 *
 * The interface doesn't know (or care) whether the handler is sync or async.
 * That's the power of Nexus: the calling side is insulated from the implementation detail.
 *
 * METAPHOR: A valet parking ticket.
 *   You hand over your car (the request) and get a ticket (the operation handle).
 *   The valet might park it immediately or queue it for later — you don't care.
 *   You claim it when you're ready.
 */
@Service
public interface ComplianceNexusService {

    /**
     * Start a 3-phase compliance investigation for the given transaction.
     *
     * Called by: PaymentProcessingWorkflow (Payments side)
     * Handled by: ComplianceNexusServiceImpl (Compliance side)
     *
     * The handler starts a ComplianceInvestigationWorkflow asynchronously.
     * The payment workflow holds a NexusOperationHandle and awaits the result
     * when it's ready — without blocking a thread.
     */
    @Operation
    InvestigationResult investigate(InvestigationRequest request);
}
