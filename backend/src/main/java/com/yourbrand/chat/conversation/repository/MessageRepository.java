package com.yourbrand.chat.conversation.repository;

import com.yourbrand.chat.conversation.entity.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdAndSequenceGreaterThanAndSequenceLessThanOrderBySequenceAsc(
            UUID conversationId, long afterSequence, long beforeSequence, Limit limit);

    List<Message> findByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId, Pageable pageable);

    /**
     * Messages strictly older than the anchor; the id breaks ties between equal timestamps.
     * The anchor is resolved in a subquery, so an anchor outside the conversation yields no rows.
     */
    @Query(value = """
            select m.*
            from message m
            where m.conversation_id = :conversationId
              and (m.created_at, m.id) < (
                  select a.created_at, a.id
                  from message a
                  where a.id = :beforeMessageId and a.conversation_id = :conversationId)
            order by m.created_at desc, m.id desc
            """, nativeQuery = true)
    List<Message> findOlder(
            @Param("conversationId") UUID conversationId,
            @Param("beforeMessageId") UUID beforeMessageId,
            Pageable pageable);
}
