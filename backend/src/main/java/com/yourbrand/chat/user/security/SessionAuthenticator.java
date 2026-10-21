package com.yourbrand.chat.user.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Stores an authenticated user in the HTTP session, which is how the SPA stays logged in. */
@Component
public class SessionAuthenticator {

    private final SecurityContextRepository securityContextRepository;

    SessionAuthenticator(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public void establish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Prevent session fixation: issue a new session id on login.
        request.getSession(true);
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);
    }
}
