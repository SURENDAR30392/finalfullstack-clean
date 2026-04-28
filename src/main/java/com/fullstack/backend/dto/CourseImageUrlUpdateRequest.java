package com.fullstack.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseImageUrlUpdateRequest(
        @NotBlank String imageUrl,
        @NotNull Long updatedBy
) {
}
