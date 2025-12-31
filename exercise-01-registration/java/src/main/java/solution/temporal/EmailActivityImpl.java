package solution.temporal;

import solution.domain.Email;

public class EmailActivityImpl implements EmailActivity{
    private EmailService emailService;

    public EmailActivityImpl(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void sendWelcomeEmail(Email email) throws InterruptedException {
        emailService.sendWelcomeEmail(email.email, email.username);
    }
}
