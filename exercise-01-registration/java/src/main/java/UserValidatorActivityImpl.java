import domain.User;

public class UserValidatorActivityImpl implements UserValidatorActivity {

    private final UserValidator userValidator;

    public UserValidatorActivityImpl(UserValidator userValidator) {
        this.userValidator = userValidator;
    }

    @Override
    public boolean validateUserData(User user) throws InterruptedException {
        return userValidator.validateUserData(user);
    }
}