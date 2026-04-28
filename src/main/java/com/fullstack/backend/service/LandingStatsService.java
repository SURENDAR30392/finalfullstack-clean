package com.fullstack.backend.service;

import com.fullstack.backend.dto.LandingStatsResponse;
import com.fullstack.backend.repository.CourseRepository;
import com.fullstack.backend.repository.EnrollmentRepository;
import com.fullstack.backend.repository.UserRepository;
import com.fullstack.backend.repository.VideoRepository;
import org.springframework.stereotype.Service;

@Service
public class LandingStatsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final VideoRepository videoRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LandingStatsService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            VideoRepository videoRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.videoRepository = videoRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public LandingStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long totalVideos = videoRepository.count();
        long totalEnrollments = enrollmentRepository.count();

        long approvedVideos = videoRepository.countByApprovalStatus("APPROVED");
        long pendingVideos = videoRepository.countByApprovalStatus("PENDING");
        long pendingCourses = courseRepository.countByApprovalStatus("PENDING");

        long creatorsCount = userRepository.countByRole("CREATOR");
        long instructorsCount = userRepository.countByRole("INSTRUCTOR");
        long studentsCount = userRepository.countByRole("STUDENT");
        long adminsCount = userRepository.countByRole("ADMIN");

        return new LandingStatsResponse(
                totalUsers,
                totalCourses,
                totalVideos,
                totalEnrollments,
                approvedVideos,
                pendingVideos,
                pendingCourses,
                creatorsCount,
                instructorsCount,
                studentsCount,
                adminsCount
        );
    }
}
