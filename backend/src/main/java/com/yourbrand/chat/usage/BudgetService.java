package com.yourbrand.chat.usage;

import com.yourbrand.chat.config.ChatProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Tracks per-user spend and enforces the daily budget (days are counted in {@code app.chat.budget-zone}). */
@Service
public class BudgetService {

    public record DailyUsage(BigDecimal spentUsd, BigDecimal budgetUsd) {
        public boolean exhausted() {
            return spentUsd.compareTo(budgetUsd) >= 0;
        }
    }

    private final UsageRecordRepository records;
    private final ChatProperties properties;
    private final Clock clock;

    BudgetService(UsageRecordRepository records, ChatProperties properties, Clock clock) {
        this.records = records;
        this.properties = properties;
        this.clock = clock;
    }

    public DailyUsage today(UUID userId) {
        var startOfDay = LocalDate.now(clock.withZone(properties.budgetZone()))
                .atStartOfDay(properties.budgetZone())
                .toInstant();
        return new DailyUsage(records.sumCostSince(userId, startOfDay), properties.dailyBudgetUsd());
    }

    public void record(UUID userId, String model, BigDecimal costUsd) {
        records.save(new UsageRecord(userId, model, costUsd));
    }
}
