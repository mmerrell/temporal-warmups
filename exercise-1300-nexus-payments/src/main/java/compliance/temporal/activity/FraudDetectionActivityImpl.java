package compliance.temporal.activity;

import compliance.FraudDetectionAgent;
import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;

/**
 * YOUR TURN: Implement the fraud detection activity.
 *
 * This is a thin bridge between Temporal and the existing FraudDetectionAgent.
 * The pattern is the same as previous exercises:
 *   1. Accept the FraudDetectionAgent via constructor (dependency injection)
 *   2. Delegate to fraudAgent.screenTransaction(request)
 *   3. Return the result
 *
 * TIP: Activities are where non-deterministic operations live.
 *      The FraudDetectionAgent calls the OpenAI API — that's I/O,
 *      which means it belongs in an activity, NOT in workflow code.
 *
 * HINT: This is identical to the activity pattern from Exercises 01-04.
 *       The only difference is the domain types (RiskScreeningRequest/Result).
 */
public class FraudDetectionActivityImpl implements FraudDetectionActivity {

    // TODO: Store the FraudDetectionAgent as a field
    private final FraudDetectionAgent fraudAgent;

    // Constructor accepts the agent (dependency injection pattern)
    public FraudDetectionActivityImpl(FraudDetectionAgent fraudAgent) {
        this.fraudAgent = fraudAgent;
    }

    @Override
    public RiskScreeningResult screenTransaction(RiskScreeningRequest request) {
        // TODO: Delegate to the fraud agent and return the result
        //   - Call fraudAgent.screenTransaction(request)
        //   - Optionally log: "[FraudDetectionActivity] Screening: " + request.getTransactionId()
        return fraudAgent.screenTransaction(request);
    }
}
