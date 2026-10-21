package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.exception.NotFoundException;
import com.yourbrand.chat.conversation.entity.Conversation;
import com.yourbrand.chat.conversation.repository.ConversationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository conversations;
    private final ModelCatalogService models;

    ConversationService(ConversationRepository conversations, ModelCatalogService models) {
        this.conversations = conversations;
        this.models = models;
    }

    public List<Conversation> getList(UUID userId) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Conversation create(UUID userId, String model) {
        String chosen = model == null ? models.defaultModel().id() : models.requireSupported(model);
        return conversations.save(new Conversation(userId, chosen));
    }

    @Transactional
    public Conversation rename(UUID userId, UUID conversationId, String title) {
        var conversation = getConversationById(userId, conversationId);
        conversation.rename(title.strip());
        return conversation;
    }

    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        conversations.delete(getConversationById(userId, conversationId));
    }

    /** Other users' conversations are reported as missing rather than forbidden, so ids cannot be probed. */
    public Conversation getConversationById(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }
}
