package com.yourbrand.chat.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.openrouter")
public record OpenRouterProperties(
        URI baseUrl,
        @NotBlank(message = "Set OPENROUTER_API_KEY (see backend/.env.example)") String apiKey,
        String appName,
        String appUrl) {}
