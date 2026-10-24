package com.yourbrand.chat.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(@NotBlank String content, @NotBlank String model) {}
