package exercise.llm;

/**
 * Options for LLM chat completion requests.
 */
public class LlmOptions {
    public String model;
    public double temperature;
    public String responseFormat;  // "json_object" for JSON-only responses

    // No-arg constructor
    public LlmOptions() {}

    public LlmOptions(String model, double temperature, String responseFormat) {
        this.model = model;
        this.temperature = temperature;
        this.responseFormat = responseFormat;
    }
}
