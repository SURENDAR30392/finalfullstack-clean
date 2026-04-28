package com.fullstack.backend.dto;

public record FrontendVideoPayload(
        String title,
        String topic,
        String youtubeLink,
        String videoId
) {
}
