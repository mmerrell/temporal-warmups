package exercise.llm;

import java.util.List;

/**
 * Interface for LLM (Large Language Model) clients.
 * This abstraction allows for easy swapping of LLM providers.
 */
public interface LlmClient {
    /**
     * Send a chat completion request to the LLM.
     *
     * @param messages List of messages (system, user, assistant)
     * @param options LLM options (model, temperature, response format)
     * @return The LLM's response content as a string
     */
    String chat(List<Message> messages, LlmOptions options);
}
