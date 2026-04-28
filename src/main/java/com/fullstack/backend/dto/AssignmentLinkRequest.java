package com.fullstack.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignmentLinkRequest(
        @NotBlank String assignmentUrl
) {
}
