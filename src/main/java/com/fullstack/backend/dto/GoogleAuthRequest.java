package com.fullstack.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String role,
        String imageUrl
) {
}
