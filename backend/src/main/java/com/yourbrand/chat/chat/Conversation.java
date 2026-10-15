package com.yourbrand.chat.chat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Conversation {

    static final String DEFAULT_TITLE = "New chat";
    private static final int TITLE_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String title = DEFAULT_TITLE;
    private String model;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = createdAt;

    protected Conversation() {}

    Conversation(UUID userId, String model) {
        this.userId = userId;
        this.model = model;
    }

    void rename(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    /** Remembers the model in use and names the conversation after its first message. */
    void recordUserMessage(String model, String content) {
        this.model = model;
        if (DEFAULT_TITLE.equals(title)) {
            String singleLine = content.strip().replaceAll("\\s+", " ");
            title = singleLine.length() <= TITLE_LENGTH ? singleLine : singleLine.substring(0, TITLE_LENGTH - 1) + "…";
        }
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getModel() {
        return model;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
