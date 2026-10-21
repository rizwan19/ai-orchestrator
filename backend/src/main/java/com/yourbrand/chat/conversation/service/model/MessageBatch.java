package com.yourbrand.chat.conversation.service.model;

import com.yourbrand.chat.conversation.entity.Message;
import java.util.List;
import java.util.UUID;

/** One page of history, oldest first; {@code nextCursor} is null once the history is exhausted. */
public record MessageBatch(List<Message> items, UUID nextCursor) {}
