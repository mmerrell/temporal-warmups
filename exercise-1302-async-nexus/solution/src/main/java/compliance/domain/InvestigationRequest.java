package compliance.domain;

/**
 * [GIVEN] Input to a compliance investigation.
 * Sent by the Payments team via Nexus to the Compliance team.
 */
public class InvestigationRequest {
    public String transactionId;
    public double amount;
    public String senderCountry;
    public String receiverCountry;
    public String description;

    public InvestigationRequest() {}

    public InvestigationRequest(String transactionId, double amount,
                                String senderCountry, String receiverCountry,
                                String description) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.senderCountry = senderCountry;
        this.receiverCountry = receiverCountry;
        this.description = description;
    }

    public String getTransactionId()   { return transactionId; }
    public double getAmount()          { return amount; }
    public String getSenderCountry()   { return senderCountry; }
    public String getReceiverCountry() { return receiverCountry; }
    public String getDescription()     { return description; }
}
