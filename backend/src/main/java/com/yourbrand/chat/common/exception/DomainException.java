package com.yourbrand.chat.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A business rule violation raised by the service layer. The message is safe to show to users;
 * {@link com.yourbrand.chat.common.web.GlobalExceptionHandler} turns it into a problem-details response.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public abstract HttpStatus status();
}
