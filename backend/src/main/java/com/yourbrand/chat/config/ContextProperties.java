package com.yourbrand.chat.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Token budgets (local estimates, see {@code TokenEstimator}) for reply prompts and history summaries. */
@Validated
@ConfigurationProperties("app.chat.context")
public record ContextProperties(
        @Positive int maxInputTokens,
        @Positive int summaryTargetTokens,
        @Positive int summaryOutputLimit) {

    public ContextProperties {
        if (summaryTargetTokens > summaryOutputLimit) {
            throw new IllegalArgumentException("app.chat.context.summary-target-tokens must not exceed summary-output-limit.");
        }
        if (summaryOutputLimit >= maxInputTokens) {
            throw new IllegalArgumentException("app.chat.context.summary-output-limit must be below max-input-tokens.");
        }
    }
}
