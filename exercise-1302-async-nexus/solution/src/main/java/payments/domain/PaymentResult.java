package payments.domain;

/**
 * [GIVEN] Result of a payment workflow execution.
 *
 * status values:
 *   "COMPLETED"           — payment processed successfully
 *   "REJECTED"            — failed payment validation
 *   "BLOCKED_COMPLIANCE"  — compliance investigation returned blocked=true
 *   "FAILED"              — unexpected error
 */
public class PaymentResult {
    public boolean success;
    public String transactionId;
    public String status;
    public String riskLevel;          // from compliance investigation: "LOW", "MEDIUM", "CRITICAL"
    public String summary;            // from compliance investigation AI summary
    public String confirmationNumber; // set when status = COMPLETED
    public String error;

    public PaymentResult() {}

    public PaymentResult(boolean success, String transactionId, String status,
                         String riskLevel, String summary,
                         String confirmationNumber, String error) {
        this.success = success;
        this.transactionId = transactionId;
        this.status = status;
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.confirmationNumber = confirmationNumber;
        this.error = error;
    }

    public boolean isSuccess()             { return success; }
    public String getTransactionId()       { return transactionId; }
    public String getStatus()              { return status; }
    public String getRiskLevel()           { return riskLevel; }
    public String getSummary()             { return summary; }
    public String getConfirmationNumber()  { return confirmationNumber; }
    public String getError()               { return error; }

    @Override
    public String toString() {
        return String.format("PaymentResult{txn=%s, status=%s, risk=%s, confirm=%s, error=%s}",
                transactionId, status, riskLevel, confirmationNumber, error);
    }
}
