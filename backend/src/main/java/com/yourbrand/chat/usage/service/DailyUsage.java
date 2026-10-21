package com.yourbrand.chat.usage.service;

import java.math.BigDecimal;

/** A user's spend so far today against the daily budget. */
public record DailyUsage(BigDecimal spentUsd, BigDecimal budgetUsd) {

    public boolean exhausted() {
        return spentUsd.compareTo(budgetUsd) >= 0;
    }
}
