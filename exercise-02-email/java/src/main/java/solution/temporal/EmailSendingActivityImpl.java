package solution.temporal;

public class EmailSendingActivityImpl implements EmailSendingActivity {
    private final EmailVerifier emailVerifier;

    public EmailSendingActivityImpl(EmailVerifier emailVerifier){
        this.emailVerifier = emailVerifier;
    }

    @Override
    public String sendVerificationEmail(String email, String token) {
        return emailVerifier.sendVerificationEmail(email, token);
    }
}
