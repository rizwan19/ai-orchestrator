package com.yourbrand.chat.common.exception;

import org.springframework.http.HttpStatus;

/** The user has spent today's budget. */
public class BudgetExceededException extends DomainException {

    public BudgetExceededException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
