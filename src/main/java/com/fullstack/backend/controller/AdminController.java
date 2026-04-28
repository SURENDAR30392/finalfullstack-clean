package com.fullstack.backend.controller;

import com.fullstack.backend.dto.AdminAnalyticsBucketResponse;
import com.fullstack.backend.dto.AdminAnalyticsResponse;
import com.fullstack.backend.dto.AdminPendingContentResponse;
import com.fullstack.backend.dto.AdminReportItemResponse;
import com.fullstack.backend.dto.AdminReportResponse;
import com.fullstack.backend.dto.AdminTopCourseResponse;
import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.dto.CourseApprovalUpdateRequest;
import com.fullstack.backend.dto.CourseResponse;
import com.fullstack.backend.dto.PendingVideoApprovalResponse;
import com.fullstack.backend.dto.UserResponse;
import com.fullstack.backend.dto.VideoApprovalUpdateRequest;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.exception.BadRequestException;
import com.fullstack.backend.repository.CourseRepository;
import com.fullstack.backend.repository.EnrollmentRepository;
import com.fullstack.backend.repository.UserRepository;
import com.fullstack.backend.repository.VideoRepository;
import com.fullstack.backend.service.CourseService;
import com.fullstack.backend.service.ReportExcelService;
import com.fullstack.backend.service.UserService;
import com.fullstack.backend.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserService userService;
    private final CourseService courseService;
    private final VideoService videoService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final VideoRepository videoRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Autowired
    private ReportExcelService reportExcelService;

    public AdminController(
            UserService userService,
            CourseService courseService,
            VideoService videoService,
            UserRepository userRepository,
            CourseRepository courseRepository,
            VideoRepository videoRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.userService = userService;
        this.courseService = courseService;
        this.videoService = videoService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.videoRepository = videoRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/courses")
    public List<CourseResponse> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/instructors")
    public List<UserResponse> getAllInstructors() {
        return userService.getUsersByRole("INSTRUCTOR");
    }

    @GetMapping("/students")
    public List<UserResponse> getAllStudents() {
        return userService.getUsersByRole("STUDENT");
    }

    @GetMapping("/pending-content")
    public List<AdminPendingContentResponse> getPendingContent() {
        List<AdminPendingContentResponse> items = new ArrayList<>();

        courseService.getPendingCourses().forEach(course -> items.add(new AdminPendingContentResponse(
                "course-" + course.id(),
                course.id(),
                "COURSE",
                course.title(),
                "Instructor: " + course.createdByName() + " | Videos: " + (course.videos() == null ? 0 : course.videos().size()),
                course.approvalStatus()
        )));

        videoService.getPendingVideoApprovals().forEach(video -> items.add(new AdminPendingContentResponse(
                "video-" + video.id(),
                video.id(),
                "VIDEO",
                video.title(),
                "Course: " + video.courseTitle() + " | Instructor: " + video.instructorName(),
                video.approvalStatus()
        )));

        items.sort(Comparator.comparing(AdminPendingContentResponse::id).reversed());
        return items;
    }

    @GetMapping("/analytics")
    public AdminAnalyticsResponse getAnalytics() {
        List<UserResponse> users = userService.getAllUsers();
        List<CourseResponse> courses = courseService.getAllCourses();
        List<PendingVideoApprovalResponse> pendingVideos = videoService.getPendingVideoApprovals();
        List<CourseResponse> pendingCourses = courseService.getPendingCourses();

        long totalVideos = courses.stream().mapToLong(course -> course.videos() == null ? 0 : course.videos().size()).sum();
        long totalEnrollments = courses.stream().mapToLong(course -> course.enrolledStudentIds() == null ? 0 : course.enrolledStudentIds().size()).sum();

        List<AdminAnalyticsBucketResponse> roleBuckets = List.of("ADMIN", "INSTRUCTOR", "STUDENT", "CREATOR")
                .stream()
                .map(role -> new AdminAnalyticsBucketResponse(
                        role,
                        users.stream().filter(user -> role.equalsIgnoreCase(user.role())).count()
                ))
                .toList();

        List<AdminAnalyticsBucketResponse> courseStatusBuckets = List.of("APPROVED", "PENDING", "REJECTED")
                .stream()
                .map(status -> new AdminAnalyticsBucketResponse(
                        status,
                        courses.stream().filter(course -> status.equalsIgnoreCase(course.approvalStatus())).count()
                ))
                .toList();

        List<AdminAnalyticsBucketResponse> videoStatusBuckets = List.of("APPROVED", "PENDING", "REJECTED")
                .stream()
                .map(status -> new AdminAnalyticsBucketResponse(
                        status,
                        courses.stream()
                                .flatMap(course -> (course.videos() == null ? List.<com.fullstack.backend.dto.VideoResponse>of() : course.videos()).stream())
                                .filter(video -> status.equalsIgnoreCase(video.approvalStatus()))
                                .count()
                ))
                .toList();

        List<AdminTopCourseResponse> topCourses = courses.stream()
                .sorted(Comparator.comparingInt((CourseResponse course) -> course.videos() == null ? 0 : course.videos().size()).reversed())
                .limit(6)
                .map(course -> new AdminTopCourseResponse(
                        course.id(),
                        course.title(),
                        course.videos() == null ? 0 : course.videos().size(),
                        course.enrolledStudentIds() == null ? 0 : course.enrolledStudentIds().size()
                ))
                .toList();

        return new AdminAnalyticsResponse(
                users.size(),
                courses.size(),
                totalVideos,
                totalEnrollments,
                pendingCourses.size() + pendingVideos.size(),
                pendingCourses.size(),
                pendingVideos.size(),
                roleBuckets,
                courseStatusBuckets,
                videoStatusBuckets,
                topCourses
        );
    }

    @GetMapping("/reports")
    public List<AdminReportResponse> getReports() {
        List<UserResponse> users = userService.getAllUsers();
        List<CourseResponse> courses = courseService.getAllCourses();
        List<CourseResponse> pendingCourses = courseService.getPendingCourses();
        List<PendingVideoApprovalResponse> pendingVideos = videoService.getPendingVideoApprovals();

        long totalVideos = courses.stream().mapToLong(course -> course.videos() == null ? 0 : course.videos().size()).sum();
        long totalEnrollments = courses.stream().mapToLong(course -> course.enrolledStudentIds() == null ? 0 : course.enrolledStudentIds().size()).sum();
        long totalStudents = users.stream().filter(user -> "STUDENT".equalsIgnoreCase(user.role())).count();
        long totalInstructors = users.stream().filter(user -> "INSTRUCTOR".equalsIgnoreCase(user.role())).count();
        long totalCreators = users.stream().filter(user -> "CREATOR".equalsIgnoreCase(user.role())).count();
        long approvedCourses = courses.stream().filter(course -> "APPROVED".equalsIgnoreCase(course.approvalStatus())).count();
        long rejectedCourses = courses.stream().filter(course -> "REJECTED".equalsIgnoreCase(course.approvalStatus())).count();

        List<Map<String, Object>> instructorRows = users.stream()
                .filter(user -> "INSTRUCTOR".equalsIgnoreCase(user.role()))
                .map(instructor -> {
                    List<CourseResponse> ownedCourses = courses.stream()
                            .filter(course -> course.createdById() != null && course.createdById().equals(instructor.id()))
                            .toList();

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", instructor.id());
                    row.put("name", instructor.name());
                    row.put("email", instructor.email());
                    row.put("courses", ownedCourses.size());
                    row.put("videos", ownedCourses.stream().mapToLong(course -> course.videos() == null ? 0 : course.videos().size()).sum());
                    row.put("enrollments", ownedCourses.stream().mapToLong(course -> course.enrolledStudentIds() == null ? 0 : course.enrolledStudentIds().size()).sum());
                    return row;
                })
                .toList();

        List<Map<String, Object>> courseRows = courses.stream()
                .map(course -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", course.id());
                    row.put("title", course.title());
                    row.put("instructor", course.createdByName());
                    row.put("status", course.approvalStatus());
                    row.put("videos", course.videos() == null ? 0 : course.videos().size());
                    row.put("enrollments", course.enrolledStudentIds() == null ? 0 : course.enrolledStudentIds().size());
                    return row;
                })
                .toList();

        return List.of(
                new AdminReportResponse(
                        "overview",
                        "Platform Summary",
                        List.of(
                                new AdminReportItemResponse("Total Users", users.size()),
                                new AdminReportItemResponse("Total Students", totalStudents),
                                new AdminReportItemResponse("Total Instructors", totalInstructors),
                                new AdminReportItemResponse("Total Creators", totalCreators),
                                new AdminReportItemResponse("Total Courses", courses.size()),
                                new AdminReportItemResponse("Approved Courses", approvedCourses),
                                new AdminReportItemResponse("Rejected Courses", rejectedCourses),
                                new AdminReportItemResponse("Pending Courses", pendingCourses.size()),
                                new AdminReportItemResponse("Pending Videos", pendingVideos.size()),
                                new AdminReportItemResponse("Total Videos", totalVideos),
                                new AdminReportItemResponse("Total Enrollments", totalEnrollments),
                                new AdminReportItemResponse("Pending Approvals", pendingCourses.size() + pendingVideos.size())
                        ),
                        List.of()
                ),
                new AdminReportResponse(
                        "instructors",
                        "Instructor Report",
                        List.of(),
                        instructorRows
                ),
                new AdminReportResponse(
                        "courses",
                        "Course Report",
                        List.of(),
                        courseRows
                )
        );
    }

    @GetMapping("/reports/download")
    public ResponseEntity<byte[]> downloadReports() throws IOException {
        int users = userRepository.findAll().size();
        int courses = courseRepository.findAll().size();
        int videos = videoRepository.findAll().size();
        int enrollments = enrollmentRepository.findAll().size();

        byte[] excel = reportExcelService.generateSimpleReport(users, courses, videos, enrollments);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "reports.xlsx");
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok()
                .headers(headers)
                .body(excel);
    }

    @PostMapping("/approve/{contentId}")
    public ApiMessageResponse approveContent(@PathVariable String contentId, Authentication authentication) {
        updateContentApproval(contentId, "APPROVED", authentication);
        return new ApiMessageResponse("Content approved successfully.");
    }

    @PostMapping("/reject/{contentId}")
    public ApiMessageResponse rejectContent(@PathVariable String contentId, Authentication authentication) {
        updateContentApproval(contentId, "REJECTED", authentication);
        return new ApiMessageResponse("Content rejected successfully.");
    }

    @DeleteMapping("/users/{id}")
    public ApiMessageResponse deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return new ApiMessageResponse("User deleted successfully");
    }

    private void updateContentApproval(String contentId, String status, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BadRequestException("Admin authentication is required.");
        }

        User admin = userService.getUserEntityByEmail(authentication.getName());
        if (contentId.startsWith("course-")) {
            Long courseId = parseContentId(contentId, "course-");
            courseService.updateApprovalStatus(courseId, new CourseApprovalUpdateRequest(status, admin.getId()));
            return;
        }

        if (contentId.startsWith("video-")) {
            Long videoId = parseContentId(contentId, "video-");
            videoService.updateVideoApprovalStatus(videoId, new VideoApprovalUpdateRequest(status, admin.getId()));
            return;
        }

        throw new BadRequestException("Unsupported content id: " + contentId);
    }

    private Long parseContentId(String contentId, String prefix) {
        try {
            return Long.parseLong(contentId.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid content id: " + contentId);
        }
    }
}
