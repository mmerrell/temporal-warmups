import static java.lang.Thread.sleep;

public class UserValidator {
    public boolean validateUserData(String email, String username, String password) throws InterruptedException {
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
}
