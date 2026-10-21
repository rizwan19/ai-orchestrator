package com.yourbrand.chat.common.web;

import com.yourbrand.chat.common.exception.DomainException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates service-layer exceptions into RFC 9457 problem details ({@code detail} is what the frontend shows). */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ProblemDetail handle(DomainException e) {
        return ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
    }
}
