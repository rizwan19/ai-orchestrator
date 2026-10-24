package com.yourbrand.chat.conversation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * An immutable, cumulative summary of every message up to {@code summarySequence}. Each version folds the previous
 * summary and the next batch of messages together; only the latest version is sent to the model.
 */
@Entity
@Getter
public class ConversationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID conversationId;
    private String summary;
    private int version;
    private long summarySequence;
    /** Locally estimated tokens of {@link #summary} alone, without the prompt wrapper. */
    private int tokenCount;
    private String model;
    private final Instant createdAt = Instant.now();

    protected ConversationSummary() {}

    public ConversationSummary(
            UUID conversationId, int version, long summarySequence, String summary, int tokenCount, String model) {
        this.conversationId = conversationId;
        this.version = version;
        this.summarySequence = summarySequence;
        this.summary = summary;
        this.tokenCount = tokenCount;
        this.model = model;
    }
}
