package com.fullstack.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseApprovalUpdateRequest(
        @NotBlank String status,
        @NotNull Long updatedBy
) {
}
