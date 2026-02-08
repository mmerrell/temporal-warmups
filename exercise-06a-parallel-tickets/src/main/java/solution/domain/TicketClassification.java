package solution.domain;
public class TicketClassification {
    public String category;
    public String urgency;
    public double confidence;
    public String reasoning;

    public TicketClassification() {}

    public TicketClassification(String category, String urgency, double confidence, String reasoning) {
        this.category = category;
        this.urgency = urgency;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }
}
