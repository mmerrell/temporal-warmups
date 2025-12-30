import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Messy pre-Temporal user registration service.
 *
 * This is the "before" version - demonstrating problems with traditional approaches:
 * - No retry logic: transient failures cause complete registration failure
 * - No durability: if process crashes, all progress is lost
 * - No visibility: can't see workflow state or progress
 * - No recovery: can't resume from the point of failure
 * - All-or-nothing: can't retry just the failed step
 *
 * Compare this to the Temporal version to understand the value Temporal provides.
 */
public class RegistrationService {

    private Map<String, User> usersDb;
    private int emailSentCount;
    private Random random;

    public RegistrationService() {
        this.usersDb = new HashMap<>();
        this.emailSentCount = 0;
        this.random = new Random();
    }

    /**
     * Inner User class to hold user data
     */
    static class User {
        String email;
        String username;
        String password;  // WARNING: In real life, NEVER store plaintext passwords!
        boolean verified;
        long createdAt;

        User(String email, String username, String password) {
            this.email = email;
            this.username = username;
            this.password = password;
            this.verified = false;
            this.createdAt = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("User{email='%s', username='%s', verified=%s}",
                email, username, verified);
        }
    }

    /**
     * Result object for registration operation
     */
    static class RegistrationResult {
        boolean success;
        String userId;
        String verificationToken;
        String error;

        RegistrationResult(boolean success, String userId, String verificationToken, String error) {
            this.success = success;
            this.userId = userId;
            this.verificationToken = verificationToken;
            this.error = error;
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("RegistrationResult{success=true, userId='%s', token='%s'}",
                    userId, verificationToken);
            } else {
                return String.format("RegistrationResult{success=false, error='%s'}", error);
            }
        }
    }

    /**
     * Step 1: Validate user input
     */

    /**
     * Step 2: Create user in database
     */
    public String createUserRecord(String email, String username, String password) {
        System.out.println("Creating user record for " + username + "...");
        sleep(1000);  // Simulate database write (1s to match Python)

        // Simulate occasional database failures (10% chance)
        if (random.nextDouble() < 0.1) {
            throw new RuntimeException("Database connection timeout");
        }

        String userId = "user_" + (usersDb.size() + 1);
        User user = new User(email, username, password);
        usersDb.put(userId, user);

        System.out.println("✓ User created with ID: " + userId);
        return userId;
    }

    /**
     * Step 3: Send welcome email to new user
     */
    public boolean sendWelcomeEmail(String email, String username) {
        System.out.println("Sending welcome email to " + email + "...");
        sleep(800);  // Simulate email sending (0.8s to match Python)

        // Simulate occasional email service failures (15% chance)
        if (random.nextDouble() < 0.15) {
            throw new RuntimeException("Email service unavailable");
        }

        emailSentCount++;
        System.out.println("✓ Welcome email sent (total sent: " + emailSentCount + ")");
        return true;
    }

    /**
     * Step 4: Send verification link
     */
    public String sendVerificationEmail(String email, String userId) {
        System.out.println("Sending verification email to " + email + "...");
        sleep(800);  // Simulate email sending (0.8s to match Python)

        // Simulate occasional email service failures (15% chance)
        if (random.nextDouble() < 0.15) {
            throw new RuntimeException("Email service unavailable");
        }

        String verificationToken = "token_" + userId + "_" + System.currentTimeMillis();
        emailSentCount++;
        System.out.println("✓ Verification email sent with token: " + verificationToken);
        return verificationToken;
    }

    /**
     * Main registration flow - runs all steps in sequence
     *
     * PROBLEM: If any step fails, we lose all progress.
     * PROBLEM: No way to retry just the failed step.
     * PROBLEM: No visibility into which step failed or workflow state.
     */
    public RegistrationResult registerUser(String email, String username, String password) {
        String separator = "=".repeat(60);
        System.out.println("\n" + separator);
        System.out.println("Starting registration for " + username + " (" + email + ")");
        System.out.println(separator + "\n");

        try {
            // Step 1: Validate
            validateUserData(email, username, password);

            // Step 2: Create user
            String userId = createUserRecord(email, username, password);

            // Step 3: Send welcome email
            sendWelcomeEmail(email, username);

            // Step 4: Send verification email
            String verificationToken = sendVerificationEmail(email, userId);

            System.out.println("\n" + separator);
            System.out.println("✓ Registration complete for " + username + "!");
            System.out.println("User ID: " + userId);
            System.out.println("Verification token: " + verificationToken);
            System.out.println(separator + "\n");

            return new RegistrationResult(true, userId, verificationToken, null);

        } catch (Exception e) {
            System.out.println("\n" + separator);
            System.out.println("✗ Registration failed for " + username + ": " + e.getMessage());
            System.out.println(separator + "\n");

            return new RegistrationResult(false, null, null, e.getMessage());
        }
    }

    /**
     * Helper method to sleep without checked exception handling
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    /**
     * Usage example - demonstrates the problems with this approach
     */
    public static void main(String[] args) {
        RegistrationService service = new RegistrationService();

        // Try registering users (matching Python's test cases)
        RegistrationResult result1 = service.registerUser(
            "alice@example.com",
            "alice",
            "secure123"
        );

        RegistrationResult result2 = service.registerUser(
            "bob@example.com",
            "bob",
            "password456"
        );

        RegistrationResult result3 = service.registerUser(
            "alice2@example.com",
            "alice",
            "another_password"
        );

        // Print final statistics
        System.out.println("\n\nFinal Results:");
        System.out.println("Users in database: " + service.usersDb.size());
        System.out.println("Emails sent: " + service.emailSentCount);

        // Demonstrate the problems
        String separator = "=".repeat(60);
        System.out.println("\n" + separator);
        System.out.println("PROBLEMS WITH THIS APPROACH:");
        System.out.println(separator);
        System.out.println("1. No retry logic - transient failures cause complete failures");
        System.out.println("2. No durability - if process crashes, all state is lost");
        System.out.println("3. No visibility - can't see workflow progress in a UI");
        System.out.println("4. No recovery - can't resume from failure point");
        System.out.println("5. Manual error handling - error-prone and repetitive");
        System.out.println("6. All-or-nothing - can't retry just the failed step");
        System.out.println("\nTemporal solves all of these problems!");
        System.out.println(separator + "\n");
    }
}
