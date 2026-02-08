package solution.temporal.activity;

import solution.temporal.PIIScrubber;

public class PIIScrubberActivityImpl implements PIIScrubberActivity {
    private final PIIScrubber scrubber;

    public PIIScrubberActivityImpl(PIIScrubber piiScrubber) {
        this.scrubber = piiScrubber;
    }

    @Override
    public String scrubPII(String ticketText) {
        return scrubber.scrubPII(ticketText);
    }
}
