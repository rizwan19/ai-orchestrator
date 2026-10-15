package com.yourbrand.chat.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    record Credentials(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 128) String password) {}

    record UserResponse(String id, String email) {
        static UserResponse of(AppUserPrincipal principal) {
            return new UserResponse(principal.id().toString(), principal.email());
        }
    }

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    AuthController(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /** Issues the XSRF-TOKEN cookie the frontend echoes back on mutating requests. */
    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return UserResponse.of(principal);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    UserResponse signup(
            @Valid @RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) {
        String email = credentials.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        users.save(new AppUser(email, passwordEncoder.encode(credentials.password())));
        return login(new Credentials(email, credentials.password()), request, response);
    }

    @PostMapping("/login")
    UserResponse login(
            @Valid @RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    credentials.email().trim(), credentials.password()));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Prevent session fixation: issue a new session id on login.
        request.getSession(true);
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);

        return UserResponse.of((AppUserPrincipal) authentication.getPrincipal());
    }
}
