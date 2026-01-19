package exercise.llm;

/**
 * Represents a message in an LLM chat conversation.
 */
public class Message {
    public String role;     // "system", "user", "assistant"
    public String content;

    // No-arg constructor
    public Message() {}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
