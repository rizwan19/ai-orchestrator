package com.yourbrand.chat.common.exception;

import org.springframework.http.HttpStatus;

/** The request breaks a business rule that bean validation cannot express. */
public class BadRequestException extends DomainException {

    public BadRequestException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.BAD_REQUEST;
    }
}
