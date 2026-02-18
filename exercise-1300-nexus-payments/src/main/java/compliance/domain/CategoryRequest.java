package compliance.domain;

public class CategoryRequest {
    public String transactionId;
    public double amount;
    public String description;
    public String senderCountry;
    public String receiverCountry;

    public CategoryRequest() {}

    public CategoryRequest(String transactionId, double amount, String description,
                           String senderCountry, String receiverCountry) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.description = description;
        this.senderCountry = senderCountry;
        this.receiverCountry = receiverCountry;
    }

    public String getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getSenderCountry() { return senderCountry; }
    public String getReceiverCountry() { return receiverCountry; }
}
