package com.fullstack.backend.dto;

import com.fullstack.backend.entity.ImageUrl;

import java.time.LocalDateTime;

public record ImageUrlResponse(
        Long id,
        Long courseId,
        String courseName,
        String imageUrl,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime createdAt
) {

    public static ImageUrlResponse fromEntity(ImageUrl imageUrl) {
        return new ImageUrlResponse(
                imageUrl.getId(),
                imageUrl.getCourseId(),
                imageUrl.getCourseName(),
                imageUrl.getImageUrl(),
                imageUrl.getUploadedById(),
                imageUrl.getUploadedByName(),
                imageUrl.getCreatedAt()
        );
    }
}
