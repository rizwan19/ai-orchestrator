package com.yourbrand.chat.conversation.mapper;

import com.yourbrand.chat.conversation.dto.response.MessagePageResponse;
import com.yourbrand.chat.conversation.dto.response.MessageResponse;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.service.model.MessageBatch;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole().value(),
                message.getContent(),
                message.getModel(),
                message.getCostUsd(),
                message.getCreatedAt());
    }

    public MessagePageResponse toPageResponse(MessageBatch batch) {
        return new MessagePageResponse(
                batch.items().stream().map(this::toResponse).toList(),
                batch.nextCursor(),
                batch.nextCursor() != null);
    }
}
