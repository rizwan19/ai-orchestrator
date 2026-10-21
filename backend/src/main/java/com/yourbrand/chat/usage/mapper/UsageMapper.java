package com.yourbrand.chat.usage.mapper;

import com.yourbrand.chat.usage.dto.response.DailyUsageResponse;
import com.yourbrand.chat.usage.service.DailyUsage;
import org.springframework.stereotype.Component;

@Component
public class UsageMapper {

    public DailyUsageResponse toResponse(DailyUsage usage) {
        return new DailyUsageResponse(usage.spentUsd(), usage.budgetUsd());
    }
}
