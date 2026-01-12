package exercise.model;

import java.util.List;

/**
 * Result from a single AI agent review.
 */
public class AgentResult {
    public String agentName;        // "Code Quality", "Test Quality", "Security"
    public String riskLevel;        // "LOW", "MEDIUM", "HIGH"
    public String recommendation;   // "APPROVE", "REQUEST_CHANGES", "BLOCK"
    public List<String> findings;

    // No-arg constructor required for Jackson deserialization
    public AgentResult() {}

    public AgentResult(String agentName, String riskLevel, String recommendation, List<String> findings) {
        this.agentName = agentName;
        this.riskLevel = riskLevel;
        this.recommendation = recommendation;
        this.findings = findings;
    }
}
