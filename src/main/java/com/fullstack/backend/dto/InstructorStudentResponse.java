package com.fullstack.backend.dto;

public record InstructorStudentResponse(
        Long id,
        String name,
        String email,
        String course,
        int progress
) {
}
