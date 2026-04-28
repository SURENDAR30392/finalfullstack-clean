package com.fullstack.backend.dto;

import com.fullstack.backend.entity.ProfileUrl;

import java.time.LocalDateTime;

public record ProfileUrlResponse(
        Long id,
        Long userId,
        String userName,
        String userRole,
        String profileUrl,
        LocalDateTime createdAt
) {
    public static ProfileUrlResponse fromEntity(ProfileUrl profileUrl) {
        return new ProfileUrlResponse(
                profileUrl.getId(),
                profileUrl.getUserId(),
                profileUrl.getUserName(),
                profileUrl.getUserRole(),
                profileUrl.getProfileUrl(),
                profileUrl.getCreatedAt()
        );
    }
}
