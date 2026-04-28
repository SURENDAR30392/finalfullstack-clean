package com.fullstack.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordOtpRequest(
        @Email @NotBlank String email
) {
}
