package com.yourbrand.chat.conversation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
public class Conversation {

    static final String DEFAULT_TITLE = "New chat";
    private static final int TITLE_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String title = DEFAULT_TITLE;
    private String model;
    private long lastMessageSequence;
    private final Instant createdAt = Instant.now();
    private Instant updatedAt = createdAt;

    protected Conversation() {}

    public Conversation(UUID userId, String model) {
        this.userId = userId;
        this.model = model;
    }

    /** Callers must hold the row lock (see {@code ConversationService#lock}) so concurrent writers never share a value. */
    public long nextMessageSequence() {
        return ++lastMessageSequence;
    }

    public void rename(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public void recordUserMessage(String model, String content) {
        this.model = model;
        if (DEFAULT_TITLE.equals(title)) {
            String singleLine = content.strip().replaceAll("\\s+", " ");
            title = singleLine.length() <= TITLE_LENGTH ? singleLine : singleLine.substring(0, TITLE_LENGTH - 1) + "…";
        }
        updatedAt = Instant.now();
    }
}
