import domain.User;

import static java.lang.Thread.sleep;

public class UserValidator {

    public boolean validateUserData(User user) throws InterruptedException {
        User myUser = user;
        System.out.println("Validating user data for " + myUser.email + "...");
        sleep(500);  // Simulate validation work (0.5s to match Python)

        if (myUser.email == null || !myUser.email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (myUser.password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        System.out.println("✓ Validation passed");
        return true;
    }
}
