package compliance.temporal;

import compliance.TransactionCategorizerAgent;
import compliance.domain.CategoryRequest;
import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import compliance.domain.TransactionCategory;
import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.WorkflowClientOperationHandlers;
import shared.nexus.ComplianceNexusService;

/**
 * YOUR TURN: Implement the Nexus service handler.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  THIS IS THE KEY NEW FILE IN THIS EXERCISE
 * ═══════════════════════════════════════════════════════════════════
 *
 * This class tells Temporal HOW to handle each Nexus operation.
 * Two operations, two different handler styles:
 *
 * METAPHOR: Think of this class as a "controller" in a REST API:
 *   - The @Service interface (ComplianceNexusService) = the API route definition
 *   - This @ServiceImpl class = the controller that handles requests
 *   - ASYNC handler = "start a background job and return a ticket number"
 *   - SYNC handler = "process this request and return the result right now"
 *
 * ── Operation 1: screenTransaction (ASYNC) ──────────────────────
 *
 *   Async means: "start a workflow to handle this request."
 *   The caller (Payments team) gets a handle to track the workflow's progress.
 *
 *   Use WorkflowClientOperationHandlers.fromWorkflowMethod():
 *
 *     return WorkflowClientOperationHandlers.fromWorkflowMethod(
 *         (context, details, client, input) ->
 *             client.newWorkflowStub(
 *                 FraudDetectionWorkflow.class,
 *                 WorkflowOptions.newBuilder()
 *                     .setWorkflowId("fraud-screen-" + input.getTransactionId())
 *                     .build()
 *             )::screenTransaction
 *     );
 *
 *   What this does:
 *   1. Payments team calls screenTransaction() via Nexus
 *   2. This handler creates a FraudDetectionWorkflow stub
 *   3. Temporal starts that workflow on the Compliance side
 *   4. The workflow ID "fraud-screen-TXN-001" is visible in Temporal UI
 *   5. Payments team can track progress via the NexusOperationHandle
 *
 * ── Operation 2: categorizeTransaction (SYNC) ───────────────────
 *
 *   Sync means: "run this inline, return the result immediately."
 *   No workflow is started — the operation completes in-place.
 *
 *   Use WorkflowClientOperationHandlers.sync():
 *
 *     return WorkflowClientOperationHandlers.sync(
 *         (context, details, client, input) -> {
 *             TransactionCategorizerAgent agent = new TransactionCategorizerAgent();
 *             return agent.categorize(input);
 *         });
 *
 * ANNOTATIONS:
 *   - @ServiceImpl(service = ComplianceNexusService.class) on the class
 *   - @OperationImpl on each handler method
 *   - Handler method names MUST match the interface method names
 */
@ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {

    // TODO: Implement screenTransaction() — ASYNC handler
    //   @OperationImpl
    //   public OperationHandler<RiskScreeningRequest, RiskScreeningResult> screenTransaction() {
    //       return WorkflowClientOperationHandlers.fromWorkflowMethod(
    //           (context, details, client, input) ->
    //               client.newWorkflowStub(
    //                   FraudDetectionWorkflow.class,
    //                   WorkflowOptions.newBuilder()
    //                       .setWorkflowId("fraud-screen-" + input.getTransactionId())
    //                       .build()
    //               )::screenTransaction
    //       );
    //   }

    // TODO: Implement categorizeTransaction() — SYNC handler
    //   @OperationImpl
    //   public OperationHandler<CategoryRequest, TransactionCategory> categorizeTransaction() {
    //       return WorkflowClientOperationHandlers.sync(
    //           (context, details, client, input) -> {
    //               TransactionCategorizerAgent agent = new TransactionCategorizerAgent();
    //               return agent.categorize(input);
    //           });
    //   }
}
