package com.yourbrand.chat.conversation.dto.response;

/** SSE {@code delta} event: the next fragment of the reply. */
public record ReplyDeltaEvent(String text) {}
