package com.fullstack.backend.dto;

public record AdminTopCourseResponse(
        Long id,
        String course,
        long videos,
        long students
) {
}
