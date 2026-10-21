package com.yourbrand.chat.conversation.mapper;

import com.yourbrand.chat.conversation.dto.response.ConversationResponse;
import com.yourbrand.chat.conversation.entity.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(), conversation.getTitle(), conversation.getModel(), conversation.getUpdatedAt());
    }
}
