package com.yourbrand.chat.integration.openrouter;

import java.math.BigDecimal;

public record Completion(String content, Integer promptTokens, Integer completionTokens, BigDecimal cost) {}
