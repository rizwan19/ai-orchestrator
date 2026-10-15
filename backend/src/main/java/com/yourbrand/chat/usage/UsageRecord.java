package com.yourbrand.chat.usage;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String model;
    private BigDecimal costUsd;
    private Instant createdAt = Instant.now();

    protected UsageRecord() {}

    UsageRecord(UUID userId, String model, BigDecimal costUsd) {
        this.userId = userId;
        this.model = model;
        this.costUsd = costUsd;
    }
}
