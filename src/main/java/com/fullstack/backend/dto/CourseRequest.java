package com.fullstack.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseRequest(
        @NotBlank String title,
        String description,
        String imageUrl,
        @NotBlank String category,
        @NotNull Long createdBy
) {
}
