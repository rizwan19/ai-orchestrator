package com.yourbrand.chat.user;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Authenticated user stored in the session. Exposes the id so controllers avoid extra lookups. */
public record AppUserPrincipal(UUID id, String email, String passwordHash) implements UserDetails {

    static AppUserPrincipal of(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
