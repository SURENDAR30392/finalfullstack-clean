package com.fullstack.backend.dto;

import com.fullstack.backend.entity.VideoLink;

import java.time.LocalDateTime;

public record VideoLinkResponse(
        Long id,
        Long courseId,
        Long videoId,
        String courseName,
        String topicName,
        String instructorName,
        String videoTitle,
        String category,
        String youtubeLink,
        LocalDateTime createdAt
) {

    public static VideoLinkResponse fromEntity(VideoLink videoLink) {
        return new VideoLinkResponse(
                videoLink.getId(),
                videoLink.getCourseId(),
                videoLink.getVideoId(),
                videoLink.getCourseName(),
                videoLink.getTopicName(),
                videoLink.getInstructorName(),
                videoLink.getVideoTitle(),
                videoLink.getCategory(),
                videoLink.getYoutubeLink(),
                videoLink.getCreatedAt()
        );
    }
}
