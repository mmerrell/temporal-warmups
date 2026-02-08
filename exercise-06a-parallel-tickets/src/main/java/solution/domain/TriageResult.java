package solution.domain;

public class TriageResult {
    public TicketClassification classification;
    public boolean success;
    public String ticketId;
    public String caseId;
    public String error;
    public boolean needsHumanReview;

    public TriageResult() {}

    public TriageResult(boolean success, String ticketId, TicketClassification classification,
                        String caseId, String error, boolean needsHumanReview) {
        this.success = success;
        this.ticketId = ticketId;
        this.classification = classification;
        this.caseId = caseId;
        this.error = error;
        this.needsHumanReview = needsHumanReview;
    }
}
