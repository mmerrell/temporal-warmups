package solution.domain;

/**
 * Result object for registration operation
 */
public class RegistrationResult {
    public boolean success;
    public String userId;
    public String verificationToken;
    public String error;

    public RegistrationResult() {
    }

    public RegistrationResult(boolean success, String userId, String verificationToken, String error) {
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
