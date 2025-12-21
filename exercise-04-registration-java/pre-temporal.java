// UserRegistration.java - Original messy implementation
// This is the Java version of Exercise #1 (User Registration)

import java.util.*;
import java.time.Instant;

public class UserRegistration {

    private Map<String, User> database = new HashMap<>();
    private List<String> emailsSent = new ArrayList<>();

    public static class User {
        String userId;
        String email;
        String name;
        boolean isVerified;
        Instant createdAt;

        public User(String userId, String email, String name) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.isVerified = false;
            this.createdAt = Instant.now();
        }

        @Override
        public String toString() {
            return String.format("User{id=%s, email=%s, name=%s, verified=%s}",
                    userId, email, name, isVerified);
        }
    }

    public void registerUser(String email, String name) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Starting registration for: " + email);
        System.out.println("=".repeat(60) + "\n");

        try {
            // Step 1: Validate email
            System.out.println("Validating email: " + email);
            Thread.sleep(300);

            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Invalid email format");
            }

            if (database.containsKey(email)) {
                throw new IllegalArgumentException("Email already registered");
            }

            System.out.println("✓ Email validation passed");

            // Step 2: Create user record
            System.out.println("Creating user record...");
            Thread.sleep(500);

            // Simulate database failures (10% failure rate)
            if (Math.random() < 0.1) {
                throw new RuntimeException("Database connection failed");
            }

            String userId = "USER-" + System.currentTimeMillis();
            User user = new User(userId, email, name);
            database.put(email, user);

            System.out.println("✓ User created: " + userId);

            // Step 3: Send welcome email
            System.out.println("Sending welcome email...");
            Thread.sleep(400);

            // Simulate email service failures (15% failure rate)
            if (Math.random() < 0.15) {
                throw new RuntimeException("Email service unavailable");
            }

            String welcomeEmail = String.format(
                    "Welcome to our platform, %s!\n\n" +
                            "Your account has been created.\n" +
                            "User ID: %s\n" +
                            "Email: %s\n\n" +
                            "Thank you for joining!",
                    name, userId, email
            );

            emailsSent.add("WELCOME:" + email);
            System.out.println("✓ Welcome email sent");

            // Step 4: Send verification email
            System.out.println("Sending verification email...");
            Thread.sleep(400);

            // Simulate email service failures (15% failure rate)
            if (Math.random() < 0.15) {
                throw new RuntimeException("Email service unavailable");
            }

            String verificationToken = UUID.randomUUID().toString();
            String verificationEmail = String.format(
                    "Please verify your email address.\n\n" +
                            "Click here to verify: https://example.com/verify?token=%s\n\n" +
                            "Token expires in 24 hours.",
                    verificationToken
            );

            emailsSent.add("VERIFICATION:" + email);
            System.out.println("✓ Verification email sent");

            System.out.println("\n" + "=".repeat(60));
            System.out.println("✓ Registration completed successfully!");
            System.out.println("User: " + user);
            System.out.println("=".repeat(60) + "\n");

        } catch (Exception e) {
            System.err.println("\n" + "=".repeat(60));
            System.err.println("✗ Registration failed: " + e.getMessage());
            System.err.println("=".repeat(60) + "\n");
            throw new RuntimeException("Registration failed", e);
        }
    }

    public void printStats() {
        System.out.println("\n=== Registration System Stats ===");
        System.out.println("Total users: " + database.size());
        System.out.println("Emails sent: " + emailsSent.size());
        System.out.println("\nRegistered users:");
        for (User user : database.values()) {
            System.out.println("  " + user);
        }
    }

    public static void main(String[] args) {
        UserRegistration system = new UserRegistration();

        // Test users
        String[][] testUsers = {
                {"alice@example.com", "Alice Johnson"},
                {"bob@example.com", "Bob Smith"},
                {"charlie@example.com", "Charlie Brown"},
                {"diana@example.com", "Diana Prince"}
        };

        for (String[] userData : testUsers) {
            try {
                system.registerUser(userData[0], userData[1]);
                Thread.sleep(1000); // Pause between registrations
            } catch (Exception e) {
                System.err.println("Failed to register: " + userData[0]);
            }
        }

        system.printStats();
    }
}