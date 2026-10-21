package com.yourbrand.chat.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameConversationRequest(@NotBlank @Size(max = 120) String title) {}
