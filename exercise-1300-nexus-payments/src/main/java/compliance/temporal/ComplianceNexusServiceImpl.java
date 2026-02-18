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
 * [STUDENT IMPLEMENTS] Nexus Handler - the KEY new concept.
 *
 * This is where the Compliance team implements the Nexus service contract.
 * Think of it as: "How do we handle requests from the Payments team?"
 *
 * Two types of operation handlers:
 *
 * 1. WorkflowClientOperationHandlers.fromWorkflowMethod (ASYNC) - screenTransaction
 *    - Starts a full FraudDetectionWorkflow
 *    - Caller gets a handle to track progress
 *    - Long-running, durable, retryable
 *
 * 2. WorkflowClientOperationHandlers.sync (SYNC) - categorizeTransaction
 *    - Runs inline with access to workflow client, returns immediately
 *    - Good for quick operations
 *    - Still goes through Temporal (logged, observed)
 */
@ServiceImpl(service = ComplianceNexusService.class)
public class ComplianceNexusServiceImpl {

    @OperationImpl
    public OperationHandler<RiskScreeningRequest, RiskScreeningResult> screenTransaction() {
        // ASYNC: Start a FraudDetectionWorkflow for each screening request
        // fromWorkflowMethod maps a Nexus operation to a workflow method
        return WorkflowClientOperationHandlers.fromWorkflowMethod(
                (context, details, client, input) ->
                        client.newWorkflowStub(
                                FraudDetectionWorkflow.class,
                                WorkflowOptions.newBuilder()
                                        .setWorkflowId("fraud-screen-" + input.getTransactionId())
                                        .build()
                        )::screenTransaction
        );
    }

    @OperationImpl
    public OperationHandler<CategoryRequest, TransactionCategory> categorizeTransaction() {
        // SYNC: Quick categorization with access to workflow client
        return WorkflowClientOperationHandlers.sync((context, details, client, input) -> {
            System.out.println("[NexusHandler] Categorizing transaction: " + input.getTransactionId());
            TransactionCategorizerAgent agent = new TransactionCategorizerAgent();
            return agent.categorize(input);
        });
    }
}
