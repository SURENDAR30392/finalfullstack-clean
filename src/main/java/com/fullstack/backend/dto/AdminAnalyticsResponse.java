package com.fullstack.backend.dto;

import java.util.List;

public record AdminAnalyticsResponse(
        long totalUsers,
        long totalCourses,
        long totalVideos,
        long totalEnrollments,
        long pendingApprovals,
        long pendingCourses,
        long pendingVideos,
        List<AdminAnalyticsBucketResponse> roleBuckets,
        List<AdminAnalyticsBucketResponse> courseStatusBuckets,
        List<AdminAnalyticsBucketResponse> videoStatusBuckets,
        List<AdminTopCourseResponse> topCourses
) {
}
