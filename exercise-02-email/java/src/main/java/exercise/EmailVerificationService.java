package exercise;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

/**
 * Pre-Temporal Email Verification Service
 *
 * This demonstrates the problems with traditional approaches:
 * - No retry logic
 * - No durability
 * - No visibility
 * - Manual error handling
 */
public class EmailVerificationService {
    private final List<String> sentEmails;
    private final Random random;

    public EmailVerificationService() {
        this.sentEmails = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Step 1: Generate verification token using SecureRandom
     */
    public String generateToken(String email) {
        System.out.println("Generating token for " + email + "...");
        sleep(300);  // Simulate work

        // Use SecureRandom (Java equivalent of Python's secrets.token_urlsafe)
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        System.out.println("✓ Token generated: " + token.substring(0, 16) + "...");
        return token;
    }

    /**
     * Step 2: Send verification email (10% failure rate)
     */
    public String sendVerificationEmail(String email, String token) {
        System.out.println("Sending verification email to " + email + "...");
        sleep(500);

        // Simulate 10% failure rate
        if (random.nextDouble() < 0.1) {
            throw new RuntimeException("Email service temporarily unavailable");
        }

        String verificationLink = "https://example.com/verify?token=" + token;
        sentEmails.add(email);

        System.out.println("✓ Verification email sent to " + email);
        System.out.println("  Link: " + verificationLink.substring(0, 50) + "...");
        return verificationLink;
    }

    /**
     * Main verification flow
     */
    public VerificationResult verifyEmail(String email) {
        String separator = "==================================================";
        System.out.println("\n" + separator);
        System.out.println("Starting verification for " + email);
        System.out.println(separator + "\n");

        try {
            String token = generateToken(email);
            String link = sendVerificationEmail(email, token);

            System.out.println("\n" + separator);
            System.out.println("✓ Verification initiated for " + email);
            System.out.println(separator + "\n");

            return new VerificationResult(true, email, token, link);
        } catch (Exception e) {
            System.out.println("\n" + separator);
            System.out.println("✗ Verification failed for " + email + ": " + e.getMessage());
            System.out.println(separator + "\n");

            VerificationResult result = new VerificationResult();
            result.success = false;
            result.email = email;
            result.error = e.getMessage();
            return result;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    /**
     * Simple result class for email verification
     */
    static class VerificationResult {
        boolean success;
        String email;
        String token;
        String verificationLink;
        String error;

        VerificationResult() {}

        VerificationResult(boolean success, String email, String token, String link) {
            this.success = success;
            this.email = email;
            this.token = token;
            this.verificationLink = link;
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("Success: %s (token: %s..., link: %s...)",
                    email,
                    token.substring(0, Math.min(16, token.length())),
                    verificationLink.substring(0, Math.min(50, verificationLink.length())));
            } else {
                return String.format("Failed: %s (error: %s)", email, error);
            }
        }
    }

    public static void main(String[] args) {
        EmailVerificationService service = new EmailVerificationService();

        String[] emails = {
            "alice@example.com",
            "bob@example.com",
            "charlie@example.com"
        };

        for (String email : emails) {
            service.verifyEmail(email);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n\nSummary:");
        System.out.println("Emails sent: " + service.sentEmails.size());

        // Show problems
        String separator = "==================================================";
        System.out.println("\n" + separator);
        System.out.println("PROBLEMS WITH THIS APPROACH:");
        System.out.println(separator);
        System.out.println("1. No retry logic - 10% email failures cause complete failures");
        System.out.println("2. No durability - process crash loses all progress");
        System.out.println("3. No visibility - can't track workflow state");
        System.out.println("4. Manual error handling - fragile and repetitive");
        System.out.println("\nTemporal solves all of these!");
        System.out.println(separator + "\n");
    }
}
