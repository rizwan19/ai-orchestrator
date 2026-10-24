package com.yourbrand.chat.integration.openrouter;

import com.yourbrand.chat.common.concurrent.CancellationToken;
import java.util.List;
import java.util.function.Consumer;

/**
 * Chat completions. Both calls block until the reply finishes and abort with
 * {@link com.yourbrand.chat.common.concurrent.OperationCancelledException} once {@code cancellation} fires.
 */
public interface ChatCompletionClient {

    /** Streams a reply; text fragments are pushed to {@code onDelta}. */
    Completion stream(String model, List<ChatMessage> messages, Consumer<String> onDelta, CancellationToken cancellation);

    /** Requests a whole reply at once, capped at {@code maxCompletionTokens}. */
    CompletionResult complete(
            String model, List<ChatMessage> messages, int maxCompletionTokens, CancellationToken cancellation);
}
