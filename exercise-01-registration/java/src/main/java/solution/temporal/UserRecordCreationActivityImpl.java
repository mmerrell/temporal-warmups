package solution.temporal;

import solution.domain.User;

public class UserRecordCreationActivityImpl implements UserRecordCreationActivity{

    private final UserCreator userCreator;

    public UserRecordCreationActivityImpl(UserCreator userCreator) {
        this.userCreator = userCreator;
    }
    @Override
    public String createUserRecord(User user) throws InterruptedException {
        return userCreator.createUserRecord(user);
    }
}
