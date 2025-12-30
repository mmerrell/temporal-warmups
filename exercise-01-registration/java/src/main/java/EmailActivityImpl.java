import domain.Email;

public class EmailActivityImpl implements EmailActivity{
    private EmailService emailService;

    @Override
    public void sendWelcomeEmail(Email email) throws InterruptedException {
        emailService.sendWelcomeEmail(email.email, email.username);
    }
}
