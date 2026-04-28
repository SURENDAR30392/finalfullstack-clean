package com.fullstack.backend.dto;

import com.fullstack.backend.entity.CourseVideo;

public record PendingVideoApprovalResponse(
        Long id,
        Long courseId,
        String courseTitle,
        String instructorName,
        String title,
        String topic,
        String youtubeLink,
        String approvalStatus
) {

    public static PendingVideoApprovalResponse fromEntity(CourseVideo video) {
        return new PendingVideoApprovalResponse(
                video.getId(),
                video.getCourse().getId(),
                video.getCourse().getTitle(),
                video.getCourse().getCreatedBy().getName(),
                video.getTitle(),
                video.getTopic(),
                video.getYoutubeLink(),
                video.getApprovalStatus()
        );
    }
}
