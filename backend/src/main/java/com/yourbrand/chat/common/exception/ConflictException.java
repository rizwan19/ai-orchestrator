package com.yourbrand.chat.common.exception;

import org.springframework.http.HttpStatus;

/** The request clashes with existing data, e.g. a duplicate email. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }
}
