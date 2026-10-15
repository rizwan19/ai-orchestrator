package com.yourbrand.chat.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository)
            throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                        // SSE responses complete on an async dispatch; the original request was already authorized.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/signup", "/error")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // Cookie-based CSRF for a same-origin SPA: XSRF-TOKEN cookie, X-XSRF-TOKEN header.
                .csrf(csrf -> csrf.spa())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
