package com.yourbrand.chat.conversation.dto.response;

/** SSE {@code error} event: the reply could not be finished. */
public record ReplyErrorEvent(String message) {}
