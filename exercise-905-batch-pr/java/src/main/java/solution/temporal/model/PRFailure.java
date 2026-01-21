package solution.temporal.model;

// Info about a PR that failed to review
public class PRFailure {
    public int prIndex;
    public String prTitle;
    public String errorMessage;
    public String timestamp;

    public PRFailure(){};

    public PRFailure(PRData pr){
        this.prIndex = pr.prIndex;
        this.prTitle = pr.prTitle;
        this.errorMessage = pr.errorMessage;
        this.timestamp = pr.timestamp;
    }
}
