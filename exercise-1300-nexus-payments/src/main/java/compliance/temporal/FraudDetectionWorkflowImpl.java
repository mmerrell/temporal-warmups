package compliance.temporal;

import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import compliance.temporal.activity.FraudDetectionActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import payments.temporal.PaymentProcessingWorkflowImpl;

import java.time.Duration;

/**
 * YOUR TURN: Implement the fraud detection workflow.
 *
 * This workflow is started BY the Nexus handler when the Payments team
 * requests a fraud screening. It's a simple one-activity workflow.
 *
 * Steps:
 *   1. Create an activity stub for FraudDetectionActivity
 *   2. Call the activity's screenTransaction() method
 *   3. Return the result
 *
 * Activity Options to use:
 *   - startToCloseTimeout: 60 seconds (OpenAI calls can be slow)
 *   - retryOptions:
 *       - initialInterval: 2 seconds
 *       - backoffCoefficient: 2.0
 *       - maximumAttempts: 5
 *
 * HINT: Same pattern as Exercise 01-04 workflows.
 *       Create stub → call activity → return result.
 *
 * REMEMBER: Use Workflow.getLogger() for logging, NOT System.out.println().
 *           Workflows can be replayed, and println would repeat on every replay.
 */
public class FraudDetectionWorkflowImpl implements FraudDetectionWorkflow {

    // TODO: Create ActivityOptions with retry policy
       ActivityOptions options = ActivityOptions.newBuilder()
           .setStartToCloseTimeout(Duration.ofSeconds(60))
           .setRetryOptions(RetryOptions.newBuilder()
               .setInitialInterval(Duration.ofSeconds(2))
               .setBackoffCoefficient(2)
//                   MaximumAttempts is mainly for special cases (e.g., cost control, strict business rules)
//               .setMaximumAttempts(5)
               .build())
           .build();

    // TODO: Create the activity stub
       private final FraudDetectionActivity fraudActivity =
           Workflow.newActivityStub(FraudDetectionActivity.class, options);

    @Override
    public RiskScreeningResult screenTransaction(RiskScreeningRequest request) {
        Logger logger = Workflow.getLogger(FraudDetectionWorkflowImpl.class);

        // TODO:
        //   1. Log: "Fraud detection workflow started for: " + request.getTransactionId()
        logger.info("Fraud detection workflow started for: " + request.getTransactionId());

        //   2. Call fraudActivity.screenTransaction(request)
        // note how most activities return a result object and take in a request object.
        // this is a common pattern in Temporal.
        RiskScreeningResult result = fraudActivity.screenTransaction(request);
        //   3. Log the result
        logger.info(" Risk screening result: " + result.toString());
        //   4. Return the result
        return result;
    }
}
