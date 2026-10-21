package com.yourbrand.chat.integration.openrouter;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

/** Streams a chat completion. Blocks until the reply finishes; text fragments are pushed to {@code onDelta}. */
public interface ChatCompletionClient {

    Completion stream(String model, List<ChatMessage> messages, Consumer<String> onDelta);

    record ChatMessage(String role, String content) {}

    /** {@code cost} is in OpenRouter credits (USD). Usage fields are null if the provider did not report them. */
    record Completion(String content, Integer promptTokens, Integer completionTokens, BigDecimal cost) {}
}
