package com.fullstack.backend.dto;

public record AuthResponse(
        String message,
        String token,
        UserResponse user
) {
}
