package com.yourbrand.chat.conversation.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Entity
public class Message {

    public enum Role {
        USER("user"),
        ASSISTANT("assistant");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    @Converter(autoApply = true)
    static class RoleConverter implements AttributeConverter<Role, String> {
        @Override
        public String convertToDatabaseColumn(Role role) {
            return role.value();
        }

        @Override
        public Role convertToEntityAttribute(String value) {
            return Arrays.stream(Role.values()).filter(role -> role.value().equals(value)).findFirst().orElseThrow();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID conversationId;
    private long sequence;
    private Role role;
    private String content;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private BigDecimal costUsd;
    private Instant createdAt = Instant.now();

    protected Message() {}

    private Message(UUID conversationId, long sequence, Role role, String content, String model) {
        this.conversationId = conversationId;
        this.sequence = sequence;
        this.role = role;
        this.content = content;
        this.model = model;
    }

    public static Message user(UUID conversationId, long sequence, String content) {
        return new Message(conversationId, sequence, Role.USER, content, null);
    }

    public static Message assistant(
            UUID conversationId,
            long sequence,
            String model,
            String content,
            Integer promptTokens,
            Integer completionTokens,
            BigDecimal cost) {
        var message = new Message(conversationId, sequence, Role.ASSISTANT, content, model);
        message.promptTokens = promptTokens;
        message.completionTokens = completionTokens;
        message.costUsd = cost;
        return message;
    }

    public UUID getId() {
        return id;
    }

    /** Position within the conversation, starting at 1; orders history and marks summary boundaries. */
    public long getSequence() {
        return sequence;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getModel() {
        return model;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
