package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.exception.BadRequestException;
import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.repository.MessageRepository;
import com.yourbrand.chat.conversation.service.model.MessageBatch;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.Completion;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final ChatProperties properties;

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

    /** The latest {@code app.chat.history-limit} messages, oldest first: the context sent to the model. */
    public List<Message> recentHistory(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, Limit.of(properties.historyLimit()))
                .reversed();
    }

    public Message saveUser(UUID conversationId, String content) {
        return messageRepository.save(Message.user(conversationId, content));
    }

    public Message saveAssistant(UUID conversationId, String model, Completion completion) {
        return messageRepository.save(Message.assistant(
                conversationId,
                model,
                completion.content(),
                completion.promptTokens(),
                completion.completionTokens(),
                completion.cost()));
    }
}
