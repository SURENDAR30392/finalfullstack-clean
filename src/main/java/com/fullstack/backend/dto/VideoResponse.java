package com.fullstack.backend.dto;

import com.fullstack.backend.entity.CourseVideo;

public record VideoResponse(
        Long id,
        String title,
        String topic,
        String youtubeLink,
        String assignmentUrl,
        String approvalStatus
) {

    public static VideoResponse fromEntity(CourseVideo video) {
        return new VideoResponse(
                video.getId(),
                video.getTitle(),
                video.getTopic(),
                video.getYoutubeLink(),
                video.getAssignmentUrl(),
                video.getApprovalStatus()
        );
    }
}
