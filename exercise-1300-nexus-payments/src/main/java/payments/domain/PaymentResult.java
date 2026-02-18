package payments.domain;

public class PaymentResult {
    public boolean success;
    public String transactionId;
    public String status;          // "COMPLETED", "REJECTED", "PENDING_APPROVAL", "FAILED"
    public String riskLevel;
    public String category;
    public String confirmationNumber;
    public String error;

    public PaymentResult() {}

    public PaymentResult(boolean success, String transactionId, String status,
                         String riskLevel, String category, String confirmationNumber,
                         String error) {
        this.success = success;
        this.transactionId = transactionId;
        this.status = status;
        this.riskLevel = riskLevel;
        this.category = category;
        this.confirmationNumber = confirmationNumber;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
    public String getRiskLevel() { return riskLevel; }
    public String getCategory() { return category; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public String getError() { return error; }
}
