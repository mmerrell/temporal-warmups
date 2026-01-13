package exercise.model;

/**
 * Request body for POST /review endpoint.
 */
public class ReviewRequest {
    public String prTitle;
    public String prDescription;
    public String diff;
    public TestSummary testSummary;  // Optional

    // No-arg constructor required for Jackson deserialization
    public ReviewRequest() {}

    public ReviewRequest(String prTitle, String prDescription, String diff, TestSummary testSummary) {
        this.prTitle = prTitle;
        this.prDescription = prDescription;
        this.diff = diff;
        this.testSummary = testSummary;
    }
}
