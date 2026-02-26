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
 * METAPHOR: Think of this class as a REST controller:
 *   - ComplianceNexusService (the interface) = your API route definition
 *   - This class                             = the controller handling requests
 *   - ComplianceAgent                        = your actual business logic
 *
 * This is a SYNC handler — it runs inline and returns immediately.
 * The Payments workflow waits briefly, gets the result, and moves on.
 * No new Temporal workflow is started on the Compliance side.
 *
 * Two new annotations to learn:
 *   @ServiceImpl  — goes on the class, points to the interface it implements
 *   @OperationImpl — goes on each handler method
 *
 * IMPORTANT: The handler method name must exactly match the interface method name.
 *   Interface declares: checkCompliance(ComplianceRequest)
 *   Your handler must be named: checkCompliance()
 *
 * What to implement:
 *   1. Add @ServiceImpl annotation to the class
 *   2. Add a ComplianceAgent field and accept it via constructor
 *   3. Add @OperationImpl to the checkCompliance() method
 *   4. Return a sync handler that calls agent.checkCompliance(input)
 *      Hint: WorkflowClientOperationHandlers has a sync() factory method.
 *            The lambda it takes receives (context, details, client, input).
 *            You only need input — pass it to the agent.
 *
 * In Exercise 1300, you'll also write an ASYNC handler that starts a full
 * workflow on the Compliance side instead of running inline.
 */
// TODO: Add @ServiceImpl annotation
public class ComplianceNexusServiceImpl {

    // TODO: Add a ComplianceAgent field

    // TODO: Add a constructor that accepts ComplianceAgent

    // TODO: Add @OperationImpl annotation
    // Note: method name must match the interface — checkCompliance
    public OperationHandler<ComplianceRequest, ComplianceResult> checkCompliance() {
        // TODO: Return a sync handler that delegates to the agent
        throw new UnsupportedOperationException("TODO: implement checkCompliance handler");
    }
}
