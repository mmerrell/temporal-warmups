package solution.temporal;

import exercise.EmailVerificationService;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import solution.domain.VerificationResult;

import java.time.Duration;

public class EmailVerificationWorkflowImpl implements EmailVerificationWorkflow{
    //1. configure options
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(20))
            .setRetryOptions(RetryOptions.newBuilder()
//                    .setMaximumAttempts(3)    //not common to set this
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    //2. create activity stubs
    // What two activities do you need? Token generation and email sending...
    private final TokenGenerationActivity tokenGenerationActivity =
            Workflow.newActivityStub(TokenGenerationActivity.class,
            ACTIVITY_OPTIONS);

    private final EmailSendingActivity emailSendingActivity =
            Workflow.newActivityStub(EmailSendingActivity.class,
                    ACTIVITY_OPTIONS);

    @Override
    public VerificationResult verifyEmail(String email) {
        Workflow.getLogger(EmailVerificationWorkflowImpl.class)
                .info("Starting email verification for " + email);

        try {
            // Step 1: Generate token (activity call)
            String token = tokenGenerationActivity.generateToken(email);

            // Step 2: Send email with that token (activity call)
            String verificationLink = emailSendingActivity.sendVerificationEmail(email, token);

            Workflow.getLogger(EmailVerificationWorkflowImpl.class)
                    .info("✓ Verification complete for " + email);

            return new VerificationResult(true, email, token, verificationLink);

        } catch (Exception e) {
            Workflow.getLogger(EmailVerificationWorkflowImpl.class)
                    .error("✗ Verification failed for " + email + ": " + e.getMessage());

            VerificationResult result = new VerificationResult();
            result.success = false;
            result.email = email;
            result.error = e.getMessage();
            return result;
        }
    }
}
