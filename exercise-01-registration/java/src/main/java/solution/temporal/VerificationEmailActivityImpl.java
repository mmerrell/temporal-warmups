package solution.temporal;

import solution.domain.RegistrationResult;
import solution.domain.User;

public class VerificationEmailActivityImpl implements VerificationEmailActivity{
    private EmailService emailService;

    public VerificationEmailActivityImpl(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String sendVerificationEmail(User user, RegistrationResult registrationResult) throws InterruptedException {
        return emailService.sendVerificationEmail(user.email, registrationResult.userId);
    }
}
