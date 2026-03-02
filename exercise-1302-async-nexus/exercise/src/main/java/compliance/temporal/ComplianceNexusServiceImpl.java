package compliance.temporal;

import compliance.domain.InvestigationRequest;
import compliance.domain.InvestigationResult;
import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.WorkflowClientOperationHandlers;
import shared.nexus.ComplianceNexusService;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  TODO FILE 4 of 7: ComplianceNexusServiceImpl — Core New Concept (20 min)
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: THE KEY LESSON of this exercise. Upgrade from sync to async Nexus
 * by starting a workflow instead of running inline.
 *
 * WHAT YOU DID IN 1301 (sync — runs inline):
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  return WorkflowClientOperationHandlers.sync(                   │
 * │      (context, details, client, input) ->                       │
 * │          complianceAgent.checkCompliance(input)                 │
 * │  );                                                             │
 * └─────────────────────────────────────────────────────────────────┘
 *   The handler runs, returns immediately. Caller waits synchronously.
 *
 * WHAT YOU IMPLEMENT IN 1302 (async — starts a workflow):
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  return WorkflowClientOperationHandlers.fromWorkflowMethod(     │
 * │      (context, details, client, input) ->                       │
 * │          client.newWorkflowStub(                                │
 * │              ComplianceInvestigationWorkflow.class,             │
 * │              WorkflowOptions.newBuilder()                       │
 * │                  .setWorkflowId("investigation-"               │
 * │                      + input.getTransactionId())                │
 * │                  .build()                                       │
 * │          )::investigate                                         │
 * │  );                                                             │
 * └─────────────────────────────────────────────────────────────────┘
 *   The handler starts a workflow and returns an operation token.
 *   The caller (PaymentProcessingWorkflow) holds a NexusOperationHandle
 *   and calls handle.getResult().get() when it's ready to collect the result.
 *
 * WHAT TO IMPLEMENT:
 *
 *   1. Add @ServiceImpl(service = ComplianceNexusService.class) to the class
 *
 *   2. Add @OperationImpl to the investigate() method
 *      NOTE: The method name must exactly match the interface — "investigate"
 *      Temporal matches handler methods to @Operation interface methods by name.
 *
 *   3. Return WorkflowClientOperationHandlers.fromWorkflowMethod(...) with:
 *      - Create a workflow stub: client.newWorkflowStub(ComplianceInvestigationWorkflow.class, options)
 *      - WorkflowId: "investigation-" + input.getTransactionId()
 *      - Method reference: stub::investigate
 *
 * MENTAL MODEL:
 *   fromWorkflowMethod() doesn't run your workflow. It registers how to START it.
 *   When Temporal receives a Nexus request, it:
 *     1. Runs your lambda to get the workflow stub + method reference
 *     2. Starts the workflow
 *     3. Returns an operation token to the caller
 *   The caller polls for completion using the token (handled by Temporal automatically).
 *
 * CHECKPOINT 2 — After this file + ComplianceWorkerApp:
 *   Start both workers and check: Temporal UI → Nexus tab → compliance-endpoint = HEALTHY
 */
// TODO 1: Add @ServiceImpl annotation
public class ComplianceNexusServiceImpl {

    // TODO 2: Add @OperationImpl annotation below
    // NOTE: method name must match the interface — "investigate"
    public OperationHandler<InvestigationRequest, InvestigationResult> investigate() {
        // TODO 3: Return WorkflowClientOperationHandlers.fromWorkflowMethod(...)
        //         Lambda: (context, details, client, input) -> ...
        //           Create workflow stub for ComplianceInvestigationWorkflow
        //           with workflowId = "investigation-" + input.getTransactionId()
        //           Return stub::investigate
        return null;
    }
}
