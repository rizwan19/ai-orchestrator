package com.yourbrand.chat.user.service;

import com.yourbrand.chat.common.exception.ConflictException;
import com.yourbrand.chat.user.entity.AppUser;
import com.yourbrand.chat.user.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates an account; emails are stored trimmed and lower-cased. */
    @Transactional
    public AppUser register(String email, String password) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new ConflictException("An account with this email already exists.");
        }
        return users.save(new AppUser(normalized, passwordEncoder.encode(password)));
    }
}
