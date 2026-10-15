package com.yourbrand.chat.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.chat")
public record ChatProperties(
        @NotEmpty @Valid List<Model> models,
        @Positive BigDecimal dailyBudgetUsd,
        ZoneId budgetZone,
        @Positive int maxMessageLength,
        @Positive int historyLimit) {

    public record Model(@NotBlank String id, @NotBlank String label) {}

    public Model defaultModel() {
        return models.getFirst();
    }

    public boolean supports(String modelId) {
        return models.stream().anyMatch(model -> model.id().equals(modelId));
    }
}
