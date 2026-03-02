package compliance.domain;

public class RiskScreeningResult {
    public String transactionId;
    public String riskLevel;       // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    public double riskScore;       // 0.0 to 1.0
    public String reasoning;
    public boolean requiresApproval;
    public boolean flaggedSanctions;

    public RiskScreeningResult() {}

    public RiskScreeningResult(String transactionId, String riskLevel, double riskScore,
                                String reasoning, boolean requiresApproval, boolean flaggedSanctions) {
        this.transactionId = transactionId;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.reasoning = reasoning;
        this.requiresApproval = requiresApproval;
        this.flaggedSanctions = flaggedSanctions;
    }

    public String getTransactionId() { return transactionId; }
    public String getRiskLevel() { return riskLevel; }
    public double getRiskScore() { return riskScore; }
    public String getReasoning() { return reasoning; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public boolean isFlaggedSanctions() { return flaggedSanctions; }
}
