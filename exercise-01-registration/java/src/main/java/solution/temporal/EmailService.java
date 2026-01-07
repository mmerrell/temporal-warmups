package solution.temporal;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class EmailService {
    public void sendWelcomeEmail(String email, String username) throws InterruptedException {
        System.out.println("Sending welcome email to " + email + "...");
        sleep(800);  // Simulate email sending (0.8s to match Python)

        // Simulate occasional email service failures (15% chance)
        Random random = new Random();
        if (random.nextDouble() < 0.15) {
            throw new RuntimeException("Email service unavailable");
        }

        AtomicInteger emailSentCount = new AtomicInteger();
        emailSentCount.getAndIncrement();
        System.out.println("✓ Welcome email sent (total sent: " + emailSentCount + ")");
    }

    public String sendVerificationEmail(String email, String userId) throws InterruptedException {
        System.out.println("Sending verification email to " + email + "...");
        sleep(800);  // Simulate email sending (0.8s to match Python)

        // Simulate occasional email service failures (15% chance)
        Random random = new Random();
        if (random.nextDouble() < 0.15) {
            throw new RuntimeException("Email service unavailable");
        }

        String verificationToken = "token_" + userId + "_" + System.currentTimeMillis();
        int emailSentCount = 0;
        emailSentCount++;
        System.out.println("✓ Verification email sent with token: " + verificationToken);
        return verificationToken;
    }
}
