package com.fullstack.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseVideoRequest(
        String title,
        @NotBlank String topic,
        @NotBlank String youtubeLink,
        String assignmentUrl,
        @NotNull Long courseId
) {
}
