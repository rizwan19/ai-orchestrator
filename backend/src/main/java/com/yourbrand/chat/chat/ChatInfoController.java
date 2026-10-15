package com.yourbrand.chat.chat;

import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.usage.BudgetService;
import com.yourbrand.chat.usage.BudgetService.DailyUsage;
import com.yourbrand.chat.user.AppUserPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class ChatInfoController {

    record ModelsResponse(List<ChatProperties.Model> models, String defaultModel) {}

    private final ChatProperties properties;
    private final BudgetService budget;

    ChatInfoController(ChatProperties properties, BudgetService budget) {
        this.properties = properties;
        this.budget = budget;
    }

    @GetMapping("/models")
    ModelsResponse models() {
        return new ModelsResponse(properties.models(), properties.defaultModel().id());
    }

    @GetMapping("/usage/today")
    DailyUsage usage(@AuthenticationPrincipal AppUserPrincipal user) {
        return budget.today(user.id());
    }
}
