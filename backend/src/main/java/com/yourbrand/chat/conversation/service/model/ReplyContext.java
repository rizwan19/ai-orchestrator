package com.yourbrand.chat.conversation.service.model;

import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.ChatMessage;
import java.util.List;
import java.util.UUID;

/** Everything the streaming phase needs, resolved while the request is still synchronous. */
public record ReplyContext(UUID userId, UUID conversationId, String model, List<ChatMessage> history) {}
