package payments.domain;

public class ApprovalDecision {
    public boolean approved;
    public String reviewerName;
    public String reason;

    public ApprovalDecision() {}

    public ApprovalDecision(boolean approved, String reviewerName, String reason) {
        this.approved = approved;
        this.reviewerName = reviewerName;
        this.reason = reason;
    }

    public boolean isApproved() { return approved; }
    public String getReviewerName() { return reviewerName; }
    public String getReason() { return reason; }
}
