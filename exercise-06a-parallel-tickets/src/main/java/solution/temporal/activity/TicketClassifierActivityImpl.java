package solution.temporal.activity;

import solution.domain.TicketClassification;
import solution.temporal.TicketClassifier;

public class TicketClassifierActivityImpl implements TicketClassifierActivity {
    private final TicketClassifier classifier;

    public TicketClassifierActivityImpl(TicketClassifier ticketClassifier) {
        this.classifier = ticketClassifier;
    }

    @Override
    public TicketClassification classifyTicket(String scrubbedText) {
        return classifier.classifyTicket(scrubbedText);
    }
}
