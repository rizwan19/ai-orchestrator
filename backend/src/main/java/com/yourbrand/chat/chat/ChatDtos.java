package com.yourbrand.chat.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

final class ChatDtos {

    private ChatDtos() {}

    record ConversationResponse(UUID id, String title, String model, Instant updatedAt) {
        static ConversationResponse of(Conversation conversation) {
            return new ConversationResponse(
                    conversation.getId(), conversation.getTitle(), conversation.getModel(), conversation.getUpdatedAt());
        }
    }

    record MessageResponse(UUID id, String role, String content, String model, BigDecimal costUsd, Instant createdAt) {
        static MessageResponse of(Message message) {
            return new MessageResponse(
                    message.getId(),
                    message.getRole().value(),
                    message.getContent(),
                    message.getModel(),
                    message.getCostUsd(),
                    message.getCreatedAt());
        }
    }

    record CreateConversationRequest(String model) {}

    record RenameRequest(@NotBlank @Size(max = 120) String title) {}

    // Length is checked against app.chat.max-message-length in ChatService.
    record SendMessageRequest(@NotBlank String content, @NotBlank String model) {}
}
