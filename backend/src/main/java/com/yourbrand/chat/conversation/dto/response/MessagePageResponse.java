package com.yourbrand.chat.conversation.dto.response;

import java.util.List;
import java.util.UUID;

public record MessagePageResponse(List<MessageResponse> items, UUID nextCursor, boolean hasMore) {}
