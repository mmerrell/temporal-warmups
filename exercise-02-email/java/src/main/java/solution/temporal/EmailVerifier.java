package solution.temporal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EmailVerifier {
    List<String> sentEmails = new ArrayList<>();

    public String sendVerificationEmail(String email, String token) {
        System.out.println("Sending verification email to " + email + "...");
        sleep(500);

        // Simulate 10% failure rate
        Random random = new Random();
        if (random.nextDouble() < 0.1) {
            throw new RuntimeException("Email service temporarily unavailable");
        }

        String verificationLink = "https://example.com/verify?token=" + token;
        sentEmails.add(email);

        System.out.println("✓ Verification email sent to " + email);
        System.out.println("  Link: " + verificationLink.substring(0, 50) + "...");
        return verificationLink;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}
