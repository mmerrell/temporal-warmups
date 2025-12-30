import domain.User;

public class UserRecordCreationActivityImpl implements UserRecordCreationActivity{

    private final UserCreator userCreator;

    public UserRecordCreationActivityImpl(UserCreator userCreator) {
        this.userCreator = userCreator;
    }
    @Override
    public String createUserRecord(User user) {
        return userCreator.createUserRecord(user);
    }
}
