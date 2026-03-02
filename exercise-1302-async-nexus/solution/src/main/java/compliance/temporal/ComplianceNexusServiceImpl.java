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
 * [SOLUTION] The async Nexus handler — starts a workflow instead of running inline.
 *
 * Compare to Exercise 1301's sync handler:
 *   return WorkflowClientOperationHandlers.sync(
 *       (context, details, client, input) -> complianceAgent.checkCompliance(input)
 *   );
 *
 * This handler uses fromWorkflowMethod() instead. The lambda returns a method reference
 * to a workflow stub — Temporal uses this to start the workflow when a Nexus request arrives.
 */
@ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {

    @OperationImpl
    public OperationHandler<InvestigationRequest, InvestigationResult> investigate() {
        return WorkflowClientOperationHandlers.fromWorkflowMethod(
                (context, details, client, input) ->
                        client.newWorkflowStub(
                                ComplianceInvestigationWorkflow.class,
                                WorkflowOptions.newBuilder()
                                        .setWorkflowId("investigation-" + input.getTransactionId())
                                        .build()
                        )::investigate
        );
    }
}
