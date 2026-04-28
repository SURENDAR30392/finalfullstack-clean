package com.fullstack.backend.dto;

import java.util.List;

public record FrontendCoursePayload(
        String title,
        String description,
        String category,
        String instructor,
        String playlistId,
        List<FrontendVideoPayload> playlist
) {
}
