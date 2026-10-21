package com.yourbrand.chat.conversation.mapper;

import com.yourbrand.chat.conversation.dto.response.MessageResponse;
import com.yourbrand.chat.conversation.entity.Message;
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
}
