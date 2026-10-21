package com.yourbrand.chat.common.exception;

import org.springframework.http.HttpStatus;

/** Login failed: unknown email or wrong password. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNAUTHORIZED;
    }
}
