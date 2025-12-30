import domain.Email;
import domain.RegistrationResult;
import domain.User;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class RegistrationWorkflowImpl implements RegistrationWorkflow{
    private final UserValidatorActivity userValidatorActivity = Workflow.newActivityStub(UserValidatorActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    private UserRecordCreationActivity createUserRecordActivity = Workflow.newActivityStub(UserRecordCreationActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    private EmailActivity welcomeEmailActivity = Workflow.newActivityStub(EmailActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );
    private VerificationEmailActivity verificationEmailActivity = Workflow.newActivityStub(VerificationEmailActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    @Override
    public RegistrationResult registerUser(User user) throws InterruptedException {
        String separator = "=".repeat(60);
        System.out.println("\n" + separator);
        System.out.println("Starting registration for " + user.username + " (" + user.email + ")");
        System.out.println(separator + "\n");

        userValidatorActivity.validateUserData(user);
        String userId =createUserRecordActivity.createUserRecord(user);
        RegistrationResult result = new RegistrationResult(true, userId, null, null);
        Email theEmail = new Email(user.email, user.username);
        welcomeEmailActivity.sendWelcomeEmail(theEmail);
        result.verificationToken = verificationEmailActivity.sendVerificationEmail(user, result);
        result.success = true;
        return result;
    }
}
