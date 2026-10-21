package com.yourbrand.chat.user.mapper;

import com.yourbrand.chat.user.dto.response.UserResponse;
import com.yourbrand.chat.user.security.AppUserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(AppUserPrincipal principal) {
        return new UserResponse(principal.id().toString(), principal.email());
    }
}
