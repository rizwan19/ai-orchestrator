package com.yourbrand.chat.usage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    @Query("select coalesce(sum(u.costUsd), 0) from UsageRecord u where u.userId = :userId and u.createdAt >= :since")
    BigDecimal sumCostSince(UUID userId, Instant since);
}
