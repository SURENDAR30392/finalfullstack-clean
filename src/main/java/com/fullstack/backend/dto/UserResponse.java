package com.fullstack.backend.dto;

import com.fullstack.backend.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        String provider,
        boolean verified,
        LocalDateTime createdAt
) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                user.isVerified(),
                user.getCreatedAt()
        );
    }
}
