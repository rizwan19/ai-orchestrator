package com.yourbrand.chat.conversation.repository;

import com.yourbrand.chat.conversation.entity.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Limit limit);
}
