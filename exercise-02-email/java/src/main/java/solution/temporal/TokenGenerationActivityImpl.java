package solution.temporal;

public class TokenGenerationActivityImpl implements TokenGenerationActivity{
    private final TokenGenerator tokenGenerator;
    public TokenGenerationActivityImpl(TokenGenerator tokenGenerator){
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public String generateToken(String email) {
        return tokenGenerator.generateToken(email);
    }
}
