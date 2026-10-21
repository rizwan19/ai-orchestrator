package com.yourbrand.chat.user.service;

import com.yourbrand.chat.user.repository.AppUserRepository;
import com.yourbrand.chat.user.security.AppUserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(email)
                .map(AppUserPrincipal::of)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
