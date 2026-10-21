package com.yourbrand.chat.user.controller;

import com.yourbrand.chat.user.dto.request.CredentialsRequest;
import com.yourbrand.chat.user.dto.response.CsrfResponse;
import com.yourbrand.chat.user.dto.response.UserResponse;
import com.yourbrand.chat.user.mapper.UserMapper;
import com.yourbrand.chat.user.security.AppUserPrincipal;
import com.yourbrand.chat.user.security.SessionAuthenticator;
import com.yourbrand.chat.user.service.AuthService;
import com.yourbrand.chat.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final SessionAuthenticator sessionAuthenticator;
    private final UserMapper mapper;

    AuthController(
            UserService userService,
            AuthService authService,
            SessionAuthenticator sessionAuthenticator,
            UserMapper mapper) {
        this.userService = userService;
        this.authService = authService;
        this.sessionAuthenticator = sessionAuthenticator;
        this.mapper = mapper;
    }

    /** Issues the XSRF-TOKEN cookie the frontend echoes back on mutating requests. */
    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName());
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return mapper.toResponse(principal);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse signup(
            @Valid @RequestBody CredentialsRequest credentials, HttpServletRequest request, HttpServletResponse response) {
        var user = userService.register(credentials.email(), credentials.password());
        return login(new CredentialsRequest(user.getEmail(), credentials.password()), request, response);
    }

    @PostMapping("/login")
    UserResponse login(
            @Valid @RequestBody CredentialsRequest credentials, HttpServletRequest request, HttpServletResponse response) {
        var authentication = authService.authenticate(credentials.email(), credentials.password());
        sessionAuthenticator.establish(authentication, request, response);
        return mapper.toResponse((AppUserPrincipal) authentication.getPrincipal());
    }
}
