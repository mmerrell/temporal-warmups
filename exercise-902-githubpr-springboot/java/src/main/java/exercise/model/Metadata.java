package exercise.model;

/**
 * Metadata about the review execution.
 */
public class Metadata {
    public String generatedAt;  // ISO-8601 timestamp
    public long tookMs;         // Execution duration in milliseconds
    public String model;        // LLM model used (e.g., "gpt-4")

    // No-arg constructor required for Jackson deserialization
    public Metadata() {}

    public Metadata(String generatedAt, long tookMs, String model) {
        this.generatedAt = generatedAt;
        this.tookMs = tookMs;
        this.model = model;
    }
}
