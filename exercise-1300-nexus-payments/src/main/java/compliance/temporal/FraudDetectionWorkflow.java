package compliance.temporal;

import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * [STUDENT IMPLEMENTS] Workflow interface for fraud detection.
 *
 * This workflow is started BY the Nexus handler when the Payments team
 * calls screenTransaction(). It's a full workflow because fraud detection
 * is a long-running operation that benefits from:
 * - Activity retries (if OpenAI API fails)
 * - Visibility in Temporal UI
 * - Durability (survives worker crashes)
 */
@WorkflowInterface
public interface FraudDetectionWorkflow {

    @WorkflowMethod
    RiskScreeningResult screenTransaction(RiskScreeningRequest request);
}
