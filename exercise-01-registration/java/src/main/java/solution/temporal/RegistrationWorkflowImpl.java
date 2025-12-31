package solution.temporal;

import solution.domain.Email;
import solution.domain.RegistrationResult;
import solution.domain.User;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RegistrationWorkflowImpl implements RegistrationWorkflow{
    private final UserValidatorActivity userValidatorActivity = Workflow.newActivityStub(UserValidatorActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
//                            .setMaximumAttempts(3)    //not common
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
//                            .setMaximumAttempts(3)    //not common
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
//                            .setMaximumAttempts(3) //not common
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
    public List<RegistrationResult> registerUser(List<User> users) throws InterruptedException {
        List<RegistrationResult> results = new ArrayList<>();
        for (User user: users){
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
            results.add(result);
        }

        return results;
    }
}
