package com.yourbrand.chat.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;

// Length is checked against app.chat.max-message-length in ChatReplyService.
public record SendMessageRequest(@NotBlank String content, @NotBlank String model) {}
