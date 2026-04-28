package com.fullstack.backend.dto;

import java.util.List;

public record VideoProgressResponse(
        Long courseId,
        int totalVideos,
        int completedVideos,
        int progress,
        List<Long> completedVideoIds,
        Long lastWatchedVideoId,
        String lastWatchedTopic
) {
}
