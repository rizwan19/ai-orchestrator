package com.yourbrand.chat.conversation.dto.response;

import com.yourbrand.chat.usage.dto.response.DailyUsageResponse;

public record ReplyDoneEvent(MessageResponse message, DailyUsageResponse usage) {}
