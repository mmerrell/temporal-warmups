package solution.temporal;

import solution.domain.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.lang.Thread.sleep;

public class UserCreator {
    private Map<String, User> usersDb;

    public UserCreator() {
        usersDb = new HashMap<>();
    }
    public String createUserRecord(User user) throws InterruptedException {
        System.out.println("Creating user record for " + user.username + "...");
        sleep(1000);  // Simulate database write (1s to match Python)

        // Simulate occasional database failures (10% chance)
        Random random = new Random();
        if (random.nextDouble() < 0.1) {
            throw new RuntimeException("Database connection timeout");
        }

        String userId = "user_" + (usersDb.size() + 1);
        User newUser = new User(user.email, user.username, user.password);
        usersDb.put(userId, newUser);

        System.out.println("✓ User created with ID: " + userId);
        return userId;
    }
}
