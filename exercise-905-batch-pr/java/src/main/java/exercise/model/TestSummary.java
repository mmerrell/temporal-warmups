package exercise.model;

/**
 * Test summary information for a pull request.
 */
public class TestSummary {
    public boolean passed;
    public int totalTests;
    public int failedTests;
    public int durationMs;

    // No-arg constructor required for Jackson deserialization
    public TestSummary() {}

    public TestSummary(boolean passed, int totalTests, int failedTests, int durationMs) {
        this.passed = passed;
        this.totalTests = totalTests;
        this.failedTests = failedTests;
        this.durationMs = durationMs;
    }
}
