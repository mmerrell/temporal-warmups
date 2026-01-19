package solution.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import solution.domain.TicketClassification;

@ActivityInterface
public interface TicketClassifierActivity {
    @ActivityMethod
    TicketClassification classifyTicket(String scrubbedText);
}
