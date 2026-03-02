package compliance.domain;

/**
 * [GIVEN] Result of a 3-phase compliance investigation.
 * Returned by the Compliance team to the Payments team via async Nexus.
 *
 * Fields:
 *   blocked    — true = block the payment, false = allow it
 *   riskLevel  — "LOW", "MEDIUM", or "CRITICAL"
 *   summary    — one-line summary of all 3 investigation phases
 */
public class InvestigationResult {
    public String transactionId;
    public boolean blocked;
    public String riskLevel;    // "LOW", "MEDIUM", "CRITICAL"
    public String summary;

    public InvestigationResult() {}

    public InvestigationResult(String transactionId, boolean blocked,
                               String riskLevel, String summary) {
        this.transactionId = transactionId;
        this.blocked = blocked;
        this.riskLevel = riskLevel;
        this.summary = summary;
    }

    public String getTransactionId() { return transactionId; }
    public boolean isBlocked()       { return blocked; }
    public String getRiskLevel()     { return riskLevel; }
    public String getSummary()       { return summary; }

    @Override
    public String toString() {
        return String.format("InvestigationResult{txn=%s, blocked=%s, risk=%s, summary='%s'}",
                transactionId, blocked, riskLevel, summary);
    }
}
