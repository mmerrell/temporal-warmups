package domain;

public class User {
    public String email;
    public String username;
    public String password;  // WARNING: In real life, NEVER store plaintext passwords!
    boolean verified;
    long createdAt;

    public User(String email, String username, String password) {
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
