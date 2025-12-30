
@ActivityInterface
public interface UserDataActivity {
    @ActivityMethod
    boolean validateUserData(String email, String username, String password)
}