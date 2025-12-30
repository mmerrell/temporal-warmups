import domain.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Temporalized service
 */
public class RegistrationServiceTemporalized {

    private Map<String, User> usersDb;
    private int emailSentCount;
    private Random random;

    public RegistrationServiceTemporalized() {
        this.usersDb = new HashMap<>();
        this.emailSentCount = 0;
        this.random = new Random();
    }

    /**
     * Step 1: Validate user input
     */
    public boolean validateUserData(String email, String username, String password) {
        System.out.println("Validating user data for " + email + "...");
        sleep(500);  // Simulate validation work (0.5s to match Python)

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        System.out.println("✓ Validation passed");
        return true;
    }

    /**
     * Step 2: Create user in database
     */

    /**
     * Step 3: Send welcome email to new user
     */
    public boolean sendWelcomeEmail(String email, String username) {

    }

    /**
     * Step 4: Send verification link
     */
    public String sendVerificationEmail(String email, String userId) {

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
        RegistrationServiceTemporalized service = new RegistrationServiceTemporalized();

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
