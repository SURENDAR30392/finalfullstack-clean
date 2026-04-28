package com.fullstack.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileUrlRequest(
        @NotBlank String profileUrl
) {
}
