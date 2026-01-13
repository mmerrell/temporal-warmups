package solution.domain;

public class ApprovalRequest {
    private boolean approved;

    // Default constructor for Jackson deserialization
    public ApprovalRequest() {
    }

    public ApprovalRequest(boolean approved) {
        this.approved = approved;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
