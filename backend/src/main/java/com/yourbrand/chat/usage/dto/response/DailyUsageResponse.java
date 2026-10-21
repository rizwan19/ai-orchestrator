package com.yourbrand.chat.usage.dto.response;

import java.math.BigDecimal;

public record DailyUsageResponse(BigDecimal spentUsd, BigDecimal budgetUsd) {}
