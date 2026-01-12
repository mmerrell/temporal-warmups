package exercise.model;

import java.util.List;

/**
 * Response from the PR review API.
 */
public class ReviewResponse {
    public String overallRecommendation;  // "APPROVE", "REQUEST_CHANGES", "BLOCK"
    public List<AgentResult> agents;
    public Metadata metadata;

    // No-arg constructor required for Jackson deserialization
    public ReviewResponse() {}

    public ReviewResponse(String overallRecommendation, List<AgentResult> agents, Metadata metadata) {
        this.overallRecommendation = overallRecommendation;
        this.agents = agents;
        this.metadata = metadata;
    }
}
