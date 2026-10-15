package com.yourbrand.chat.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);
}
