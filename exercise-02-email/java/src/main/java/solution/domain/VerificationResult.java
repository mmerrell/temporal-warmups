package solution.domain;

public class VerificationResult {
    public boolean success;
    public String email;
    String token;
    String verificationLink;
    public String error;

    public VerificationResult() {}

    public VerificationResult(boolean success, String email, String token, String link) {
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
