package com.yourbrand.chat.conversation.repository;

import com.yourbrand.chat.conversation.entity.Conversation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    /** Locks the row until the transaction ends, so the message sequence counter is never written concurrently. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Conversation c
            where c.id = :id and c.userId = :userId
            """)
    Optional<Conversation> findOwnedForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findForUpdate(@Param("id") UUID id);
}
