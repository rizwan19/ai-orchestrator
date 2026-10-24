package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.concurrent.CancellationToken;
import com.yourbrand.chat.common.exception.ConflictException;
import com.yourbrand.chat.conversation.entity.ConversationSummary;
import com.yourbrand.chat.conversation.repository.ConversationSummaryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Appends summary versions; existing versions are never changed. */
@Component
@RequiredArgsConstructor
public class ConversationSummaryStore {

    private final ConversationService conversationService;
    private final ConversationSummaryRepository summaries;
    private final TransactionTemplate transaction;

    /**
     * Saves version {@code expectedVersion + 1}, provided {@code expectedVersion} is still the latest and the new
     * boundary moves past it. Conflicts are not retried, since a retry would mean another paid summary call.
     */
    public ConversationSummary append(
            UUID conversationId,
            int expectedVersion,
            long summarySequence,
            String summary,
            int tokenCount,
            String model,
            CancellationToken cancellation) {
        return transaction.execute(status -> {
            conversationService.lock(conversationId);
            cancellation.throwIfCancelled();

            var latest = summaries.findFirstByConversationIdOrderByVersionDesc(conversationId).orElse(null);
            int latestVersion = latest == null ? 0 : latest.getVersion();
            long latestSequence = latest == null ? 0 : latest.getSummarySequence();
            if (latestVersion != expectedVersion || summarySequence <= latestSequence) {
                throw new ConflictException("The conversation changed while it was being summarized. Please try again.");
            }
            return summaries.save(new ConversationSummary(
                    conversationId, expectedVersion + 1, summarySequence, summary, tokenCount, model));
        });
    }
}
