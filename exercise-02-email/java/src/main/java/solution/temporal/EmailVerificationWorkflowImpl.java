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
            .setStartToCloseTimeout(Duration.ofMinutes(30))
            .setRetryOptions(RetryOptions.newBuilder()
//                    .setMaximumAttempts(3)    //not common to set this
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
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
        String separator = "==================================================";
        System.out.println("\n" + separator);
        System.out.println("Starting verification for " + email);
        System.out.println(separator + "\n");

        try {
            String token = tokenGenerationActivity.generateToken(email);
            String link = emailSendingActivity.sendVerificationEmail(email, token);

            System.out.println("\n" + separator);
            System.out.println("✓ Verification initiated for " + email);
            System.out.println(separator + "\n");

            return new VerificationResult(true, email, token, link);
        } catch (Exception e) {
            System.out.println("\n" + separator);
            System.out.println("✗ Verification failed for " + email + ": " + e.getMessage());
            System.out.println(separator + "\n");

            VerificationResult result = new VerificationResult();
            result.success = false;
            result.email = email;
            result.error = e.getMessage();
            return result;
        }
    }
}
