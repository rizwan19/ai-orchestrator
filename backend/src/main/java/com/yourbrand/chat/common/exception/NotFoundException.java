package com.yourbrand.chat.common.exception;

import org.springframework.http.HttpStatus;

/** The requested resource does not exist or belongs to someone else. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }
}
