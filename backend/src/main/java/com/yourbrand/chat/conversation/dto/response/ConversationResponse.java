package com.yourbrand.chat.conversation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(UUID id, String title, String model, Instant updatedAt) {}
