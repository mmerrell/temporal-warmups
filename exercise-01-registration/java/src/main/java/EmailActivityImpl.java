import domain.Email;

public class EmailActivityImpl implements EmailActivity{
    private EmailService emailService;

    @Override
    public String sendWelcomeEmail(Email email) throws InterruptedException {
        return emailService.sendWelcomeEmail(email.email, email.username);
    }
}
