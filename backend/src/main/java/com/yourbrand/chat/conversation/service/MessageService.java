package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.exception.BadRequestException;
import com.yourbrand.chat.conversation.entity.Conversation;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.repository.MessageRepository;
import com.yourbrand.chat.conversation.service.model.MessageBatch;
import com.yourbrand.chat.integration.openrouter.Completion;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Up to {@code size} messages older than {@code beforeMessageId} (or the latest when null), oldest first.
     * One extra row is fetched to learn whether more history exists without a count query.
     */
    @Transactional(readOnly = true)
    public MessageBatch getPage(UUID userId, UUID conversationId, UUID beforeMessageId, int size) {
        conversationService.getConversationById(userId, conversationId);
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Message page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }

        var limit = PageRequest.of(0, size + 1);
        List<Message> rows = beforeMessageId == null
                ? messageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(conversationId, limit)
                : messageRepository.findOlder(conversationId, beforeMessageId, limit);

        boolean hasMore = rows.size() > size;
        var descending = rows.subList(0, Math.min(size, rows.size()));
        UUID nextCursor = hasMore ? descending.getLast().getId() : null;
        return new MessageBatch(List.copyOf(descending.reversed()), nextCursor);
    }

    /** {@code conversation} must be locked (see {@link ConversationService#lock}) by the surrounding transaction. */
    public Message saveMessage(Conversation conversation, String content) {
        return messageRepository.save(Message.user(conversation.getId(), conversation.nextMessageSequence(), content));
    }

    /** {@code conversation} must be locked (see {@link ConversationService#lock}) by the surrounding transaction. */
    public Message saveAssistant(Conversation conversation, String model, Completion completion) {
        return messageRepository.save(Message.assistant(
                conversation.getId(),
                conversation.nextMessageSequence(),
                model,
                completion.content(),
                completion.promptTokens(),
                completion.completionTokens(),
                completion.cost()));
    }
}
