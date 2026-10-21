package com.yourbrand.chat.conversation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, String role, String content, String model, BigDecimal costUsd, Instant createdAt) {}
