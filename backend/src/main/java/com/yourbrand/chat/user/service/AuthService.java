package com.yourbrand.chat.user.service;

import com.yourbrand.chat.common.exception.InvalidCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;

    AuthService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public Authentication authenticate(String email, String password) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email.trim(), password));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }
    }
}
