package com.yourbrand.chat.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yourbrand.chat.common.exception.BudgetExceededException;
import com.yourbrand.chat.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDomainExceptionsToProblemDetails() {
        var notFound = handler.handle(new NotFoundException("Conversation not found."));
        assertThat(notFound.getStatus()).isEqualTo(404);
        assertThat(notFound.getDetail()).isEqualTo("Conversation not found.");

        assertThat(handler.handle(new BudgetExceededException("Limit reached")).getStatus()).isEqualTo(429);
    }
}
