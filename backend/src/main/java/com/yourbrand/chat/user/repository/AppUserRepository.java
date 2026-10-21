package com.yourbrand.chat.user.repository;

import java.util.Optional;
import com.yourbrand.chat.user.entity.AppUser;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
