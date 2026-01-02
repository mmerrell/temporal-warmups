package solution.temporal;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

public class TokenGenerator {
    public String generateToken(String email) {
        System.out.println("Generating token for " + email + "...");
        sleep(300);  // Simulate work

        // Use SecureRandom (Java equivalent of Python's secrets.token_urlsafe)
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        sleep(10000);

        System.out.println("✓ Token generated: " + token.substring(0, 16) + "...");
        return token;
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
