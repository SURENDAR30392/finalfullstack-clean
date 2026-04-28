package com.fullstack.backend.dto;

public record AdminPendingContentResponse(
        String id,
        Long contentId,
        String type,
        String title,
        String subtitle,
        String approvalStatus
) {
}
