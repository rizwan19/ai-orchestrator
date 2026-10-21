package com.yourbrand.chat.conversation.dto.response;

import com.yourbrand.chat.usage.dto.response.DailyUsageResponse;

/** SSE {@code done} event: the saved reply and the user's updated spend. */
public record ReplyDoneEvent(MessageResponse message, DailyUsageResponse usage) {}
