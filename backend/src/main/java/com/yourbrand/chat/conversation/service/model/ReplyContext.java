package com.yourbrand.chat.conversation.service.model;

import com.yourbrand.chat.conversation.entity.Message;
import java.util.UUID;

/** An accepted user message awaiting its reply; {@code runId} identifies the run holding the conversation's guard. */
public record ReplyContext(UUID userId, UUID conversationId, String model, Message userMessage, UUID runId) {}
