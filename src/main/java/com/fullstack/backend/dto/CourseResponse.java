package com.fullstack.backend.dto;

import java.util.List;

public record CourseResponse(
        Long id,
        String title,
        String description,
        String category,
        String approvalStatus,
        String imageUrl,
        Long imageUploadedById,
        String imageUploadedByName,
        Long createdById,
        String createdByName,
        List<VideoResponse> videos,
        List<Long> enrolledStudentIds
) {
}
