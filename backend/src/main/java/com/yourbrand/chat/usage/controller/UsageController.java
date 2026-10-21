package com.yourbrand.chat.usage.controller;

import com.yourbrand.chat.usage.dto.response.DailyUsageResponse;
import com.yourbrand.chat.usage.mapper.UsageMapper;
import com.yourbrand.chat.usage.service.UsageService;
import com.yourbrand.chat.user.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usage")
class UsageController {

    private final UsageService usage;
    private final UsageMapper mapper;

    UsageController(UsageService usage, UsageMapper mapper) {
        this.usage = usage;
        this.mapper = mapper;
    }

    @GetMapping("/today")
    DailyUsageResponse today(@AuthenticationPrincipal AppUserPrincipal user) {
        return mapper.toResponse(usage.today(user.id()));
    }
}
