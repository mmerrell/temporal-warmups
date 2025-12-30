import domain.RegistrationResult;
import domain.User;

public class VerificationEmailActivityImpl implements VerificationEmailActivity{
    private EmailService emailService;

    @Override
    public String sendVerificationEmail(User user, RegistrationResult registrationResult) {
        return emailService.sendVerificationEmail(user.email, registrationResult.userId);
    }
}
