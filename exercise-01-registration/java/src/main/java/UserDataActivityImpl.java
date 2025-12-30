public class UserDataActivityImpl implements UserDataActivity {

    private final UserValidator userValidator;

    public UserDataActivityImpl(UserValidator userValidator) {
        this.userValidator = userValidator;
    }
    @Override
    public boolean validateUserData(String email, String username, String password) {
        return userValidator.validateUserData(email, username, password);
    }
}