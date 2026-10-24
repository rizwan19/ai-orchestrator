package com.yourbrand.chat.conversation.repository;

import com.yourbrand.chat.conversation.entity.ConversationSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, UUID> {

    Optional<ConversationSummary> findFirstByConversationIdOrderByVersionDesc(UUID conversationId);
}
