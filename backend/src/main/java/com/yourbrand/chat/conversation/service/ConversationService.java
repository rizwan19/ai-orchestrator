package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.exception.NotFoundException;
import com.yourbrand.chat.conversation.entity.Conversation;
import com.yourbrand.chat.conversation.repository.ConversationRepository;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ModelCatalogService modelCatalogService;

    public List<Conversation> getList(UUID userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Conversation create(UUID userId, String model) {
        String chosen = model == null ? modelCatalogService.defaultModel().id() : modelCatalogService.requireSupported(model);
        return conversationRepository.save(new Conversation(userId, chosen));
    }

    @Transactional
    public Conversation rename(UUID userId, UUID conversationId, String title) {
        // Locked like message writes, so this flush cannot overwrite a newer message sequence counter.
        var conversation = lockOwned(userId, conversationId);
        conversation.rename(title.strip());
        return conversation;
    }

    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        conversationRepository.delete(getConversationById(userId, conversationId));
    }

    /** Other users' conversations are reported as missing rather than forbidden, so ids cannot be probed. */
    public Conversation getConversationById(UUID userId, UUID conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }

    public Conversation lockOwned(UUID userId, UUID conversationId) {
        return conversationRepository.findOwnedForUpdate(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }

    /** Holds the row lock until the surrounding transaction ends; for internal writes whose owner was checked earlier. */
    public Conversation lock(UUID conversationId) {
        return conversationRepository.findForUpdate(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }
}
