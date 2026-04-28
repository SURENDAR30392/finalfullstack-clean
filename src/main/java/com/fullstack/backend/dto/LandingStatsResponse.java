package com.fullstack.backend.dto;

public record LandingStatsResponse(
        long totalUsers,
        long totalCourses,
        long totalVideos,
        long totalEnrollments,
        long approvedVideos,
        long pendingVideos,
        long pendingCourses,
        long creatorsCount,
        long instructorsCount,
        long studentsCount,
        long adminsCount
) {
}
