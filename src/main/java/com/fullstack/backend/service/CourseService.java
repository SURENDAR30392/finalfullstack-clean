package com.fullstack.backend.service;

import com.fullstack.backend.dto.CourseImageUrlUpdateRequest;
import com.fullstack.backend.dto.CourseApprovalUpdateRequest;
import com.fullstack.backend.dto.CourseRequest;
import com.fullstack.backend.dto.CourseResponse;
import com.fullstack.backend.dto.FrontendCoursePayload;
import com.fullstack.backend.dto.FrontendVideoPayload;
import com.fullstack.backend.dto.ImageUrlResponse;
import com.fullstack.backend.dto.LoadDataRequest;
import com.fullstack.backend.dto.VideoResponse;
import com.fullstack.backend.entity.Course;
import com.fullstack.backend.entity.CourseVideo;
import com.fullstack.backend.entity.ImageUrl;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.exception.BadRequestException;
import com.fullstack.backend.exception.ResourceNotFoundException;
import com.fullstack.backend.repository.CourseRepository;
import com.fullstack.backend.repository.EnrollmentRepository;
import com.fullstack.backend.repository.ImageUrlRepository;
import com.fullstack.backend.repository.AssignmentLinkRepository;
import com.fullstack.backend.repository.VideoProgressRepository;
import com.fullstack.backend.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class CourseService {

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    private final CourseRepository courseRepository;
    private final ImageUrlRepository imageUrlRepository;
    private final VideoRepository videoRepository;
    private final AssignmentLinkRepository assignmentLinkRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final UserService userService;
    private final VideoLinkService videoLinkService;
    private final ModelMapper modelMapper;

    public CourseService(
            CourseRepository courseRepository,
            ImageUrlRepository imageUrlRepository,
            VideoRepository videoRepository,
            AssignmentLinkRepository assignmentLinkRepository,
            EnrollmentRepository enrollmentRepository,
            VideoProgressRepository videoProgressRepository,
            UserService userService,
            VideoLinkService videoLinkService,
            ModelMapper modelMapper
    ) {
        this.courseRepository = courseRepository;
        this.imageUrlRepository = imageUrlRepository;
        this.videoRepository = videoRepository;
        this.assignmentLinkRepository = assignmentLinkRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.videoProgressRepository = videoProgressRepository;
        this.userService = userService;
        this.videoLinkService = videoLinkService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::toCourseResponse)
                .toList();
    }

    @Transactional
    public List<CourseResponse> getApprovedCourses() {
        return courseRepository.findByApprovalStatusOrderByIdDesc("APPROVED")
                .stream()
                .map(course -> toCourseResponse(course, true))
                .toList();
    }

    @Transactional
    public List<CourseResponse> getPendingCourses() {
        return courseRepository.findByApprovalStatusOrderByIdDesc("PENDING")
                .stream()
                .map(this::toCourseResponse)
                .toList();
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        User creator = userService.getUserEntity(request.createdBy());
        validateCourseManagerRole(creator, null);

        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCategory(request.category());
        course.setApprovalStatus("PENDING");
        course.setCreatedBy(creator);

        Course savedCourse = courseRepository.save(course);
        saveImageUrlRecordIfPresent(savedCourse, creator, request.imageUrl());
        videoLinkService.syncCourseVideoLinks(savedCourse);
        return toCourseResponse(savedCourse);
    }

    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseRequest request) {
        Course course = getCourseEntity(courseId);
        User creator = userService.getUserEntity(request.createdBy());
        validateCourseManagerRole(creator, course);

        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCategory(request.category());
        course.setApprovalStatus("PENDING");

        Course savedCourse = courseRepository.save(course);
        saveImageUrlRecordIfPresent(savedCourse, creator, request.imageUrl());
        videoLinkService.syncCourseVideoLinks(savedCourse);
        return toCourseResponse(savedCourse);
    }

    @Transactional
    public CourseResponse updateCourseImageUrl(Long courseId, CourseImageUrlUpdateRequest request) {
        Course course = getCourseEntity(courseId);
        User updater = userService.getUserEntity(request.updatedBy());
        validateCourseManagerRole(updater, course);
        course.setApprovalStatus("PENDING");
        Course savedCourse = courseRepository.save(course);
        saveImageUrlRecordIfPresent(savedCourse, updater, request.imageUrl());
        return toCourseResponse(savedCourse);
    }

    @Transactional
    public CourseResponse updateApprovalStatus(Long courseId, CourseApprovalUpdateRequest request) {
        Course course = getCourseEntity(courseId);
        User updater = userService.getUserEntity(request.updatedBy());
        validateAdminRole(updater);

        String normalizedStatus = request.status().trim().toUpperCase();
        if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            throw new BadRequestException("Approval status must be APPROVED or REJECTED.");
        }

        course.setApprovalStatus(normalizedStatus);
        return toCourseResponse(courseRepository.save(course));
    }

    public List<ImageUrlResponse> getAllImageUrls() {
        return imageUrlRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(imageUrl -> modelMapper.map(imageUrl, ImageUrlResponse.class))
                .toList();
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = getCourseEntity(courseId);
        videoProgressRepository.deleteByCourseId(courseId);
        assignmentLinkRepository.deleteByCourseId(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        videoRepository.deleteByCourseId(courseId);
        videoLinkService.deleteByCourseId(courseId);
        imageUrlRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }

    public Course getCourseEntity(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    @Transactional
    public List<CourseResponse> loadFrontendData(LoadDataRequest request) {
        if (request == null || request.courses() == null || request.courses().isEmpty()) {
            throw new BadRequestException("Courses payload is required.");
        }

        List<CourseResponse> savedCourses = new ArrayList<>();

        for (FrontendCoursePayload coursePayload : request.courses()) {
            User instructor = userService.getOrCreateInstructor(
                    coursePayload.instructor() == null || coursePayload.instructor().isBlank()
                            ? "Default Instructor"
                            : coursePayload.instructor()
            );

            Course course = new Course();
            course.setTitle(coursePayload.title());
            course.setDescription(defaultText(coursePayload.description(), "Imported from frontend JSON"));
            course.setCategory(defaultText(coursePayload.category(), "General"));
            course.setApprovalStatus("APPROVED");
            course.setCreatedBy(instructor);

            List<CourseVideo> videos = new ArrayList<>();
            if (coursePayload.playlist() != null) {
                for (int index = 0; index < coursePayload.playlist().size(); index++) {
                    FrontendVideoPayload videoPayload = coursePayload.playlist().get(index);
                    CourseVideo video = new CourseVideo();
                    video.setTitle(defaultText(videoPayload.title(), videoPayload.topic()));
                    video.setTopic(defaultText(videoPayload.topic(), "Untitled Topic"));
                    video.setYoutubeLink(resolveYoutubeLink(coursePayload, videoPayload, index));
                    video.setCourse(course);
                    videos.add(video);
                }
            }

            course.setVideos(videos);
            Course savedCourse = courseRepository.save(course);
            videoLinkService.syncCourseVideoLinks(savedCourse);
            savedCourses.add(toCourseResponse(savedCourse));
        }

        return savedCourses;
    }

    @Transactional
    public void seedInitialData() {
        if (courseRepository.count() != 0) {
            return;
        }

        LoadDataRequest request = new LoadDataRequest(List.of(
                new FrontendCoursePayload(
                        "Java Full Course",
                        "Core Java concepts for beginners.",
                        "Programming",
                        "Telusko",
                        "PLsyeobzWxl7pe_IiTfNyr55kwJPWbgxB5",
                        List.of(
                                new FrontendVideoPayload("Introduction to Java", "Introduction to Java", null, "java-01"),
                                new FrontendVideoPayload("Variables in Java", "Variables in Java", null, "java-02"),
                                new FrontendVideoPayload("Data Types", "Data Types", null, "java-03"),
                                new FrontendVideoPayload("Operators", "Operators", null, "java-04"),
                                new FrontendVideoPayload("Control Statements", "Control Statements", null, "java-05"),
                                new FrontendVideoPayload("Loops", "Loops", null, "java-06"),
                                new FrontendVideoPayload("Arrays", "Arrays", null, "java-07"),
                                new FrontendVideoPayload("Strings", "Strings", null, "java-08"),
                                new FrontendVideoPayload("OOP Concepts", "OOP Concepts", null, "java-09"),
                                new FrontendVideoPayload("Exception Handling", "Exception Handling", null, "java-10")
                        )
                ),
                new FrontendCoursePayload(
                        "Python Full Course",
                        "Python programming fundamentals and practical coding.",
                        "Programming",
                        "Telusko",
                        "PLsyeobzWxl7poL9JTVyndKe62ieoN-MZ3",
                        List.of(
                                new FrontendVideoPayload("Python Introduction", "Python Introduction", null, "python-01"),
                                new FrontendVideoPayload("Variables and Input", "Variables and Input", null, "python-02"),
                                new FrontendVideoPayload("Data Types", "Data Types", null, "python-03"),
                                new FrontendVideoPayload("Conditions", "Conditions", null, "python-04"),
                                new FrontendVideoPayload("Loops", "Loops", null, "python-05"),
                                new FrontendVideoPayload("Functions", "Functions", null, "python-06")
                        )
                ),
                new FrontendCoursePayload(
                        "React JS Course",
                        "Frontend React course covering components, state, and hooks.",
                        "Frontend",
                        "Telusko",
                        "PLsyeobzWxl7r2ZX1fl-7CKnayxHJA_1ol",
                        List.of(
                                new FrontendVideoPayload("React Introduction", "React Introduction", null, "react-01"),
                                new FrontendVideoPayload("Components", "Components", null, "react-02"),
                                new FrontendVideoPayload("Props and State", "Props and State", null, "react-03"),
                                new FrontendVideoPayload("Hooks", "Hooks", null, "react-04")
                        )
                ),
                new FrontendCoursePayload(
                        "Spring Boot Course",
                        "Learn Spring Boot REST APIs and JPA integration.",
                        "Backend",
                        "Telusko",
                        "PLsyeobzWxl7qbKoSgR5ub6jolI8-ocxCF",
                        List.of(
                                new FrontendVideoPayload("Spring Boot Basics", "Spring Boot Basics", null, "spring-01"),
                                new FrontendVideoPayload("REST Controller", "REST Controller", null, "spring-02"),
                                new FrontendVideoPayload("JPA Integration", "JPA Integration", null, "spring-03"),
                                new FrontendVideoPayload("Exception Handling", "Exception Handling", null, "spring-04")
                        )
                ),
                new FrontendCoursePayload(
                        "MySQL Course",
                        "Database fundamentals and SQL essentials.",
                        "Database",
                        "FreeCodeCamp",
                        null,
                        List.of(
                                new FrontendVideoPayload("MySQL Full Course", "MySQL Full Course", null, "HXV3zeQKqGY")
                        )
                ),
                new FrontendCoursePayload(
                        "HTML CSS Course",
                        "HTML and CSS from basics to full page design.",
                        "Frontend",
                        "SuperSimpleDev",
                        null,
                        List.of(
                                new FrontendVideoPayload("HTML CSS Full Course", "HTML CSS Full Course", null, "G3e-cpL7ofc")
                        )
                )
        ));

        loadFrontendData(request);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private CourseResponse toCourseResponse(Course course) {
        return toCourseResponse(course, false);
    }

    private CourseResponse toCourseResponse(Course course, boolean approvedVideosOnly) {
        Optional<ImageUrl> latestImage = imageUrlRepository.findTopByCourseIdOrderByCreatedAtDescIdDesc(course.getId());
        List<VideoResponse> videoResponses = course.getVideos()
                .stream()
                .filter(video -> !approvedVideosOnly || "APPROVED".equalsIgnoreCase(video.getApprovalStatus()))
                .map(video -> modelMapper.map(video, VideoResponse.class))
                .toList();

        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory(),
                course.getApprovalStatus(),
                latestImage.map(ImageUrl::getImageUrl).orElse(null),
                latestImage.map(ImageUrl::getUploadedById).orElse(null),
                latestImage.map(ImageUrl::getUploadedByName).orElse(null),
                course.getCreatedBy().getId(),
                course.getCreatedBy().getName(),
                videoResponses,
                course.getEnrollments().stream().map(enrollment -> enrollment.getUser().getId()).toList()
        );
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String trimmedImageUrl = imageUrl.trim();
        return trimmedImageUrl.isEmpty() ? null : trimmedImageUrl;
    }

    private void saveImageUrlRecordIfPresent(Course course, User uploader, String imageUrlValue) {
        String normalizedImageUrl = normalizeImageUrl(imageUrlValue);
        if (normalizedImageUrl == null) {
            return;
        }

        ImageUrl imageUrl = new ImageUrl();
        imageUrl.setCourseId(course.getId());
        imageUrl.setCourseName(course.getTitle());
        imageUrl.setImageUrl(normalizedImageUrl);
        imageUrl.setUploadedById(uploader.getId());
        imageUrl.setUploadedByName(uploader.getName());
        imageUrlRepository.save(imageUrl);
    }

    private void validateCourseManagerRole(User user, Course course) {
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();

        if ("ADMIN".equals(role) || "CREATOR".equals(role)) {
            return;
        }

        if ("INSTRUCTOR".equals(role)) {
            if (course == null || course.getCreatedBy().getId().equals(user.getId())) {
                return;
            }
            throw new BadRequestException("Instructors can update images only for their own courses.");
        }

        throw new BadRequestException("Only admin, instructor, or content creator can manage course images.");
    }

    private void validateAdminRole(User user) {
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        if (!"ADMIN".equals(role)) {
            throw new BadRequestException("Only admin can approve or reject content.");
        }
    }

    @Transactional
    public void markCoursePendingApproval(Long courseId) {
        Course course = getCourseEntity(courseId);
        course.setApprovalStatus("PENDING");
        courseRepository.save(course);
    }

    private String resolveYoutubeLink(FrontendCoursePayload coursePayload, FrontendVideoPayload videoPayload, int index) {
        if (videoPayload.youtubeLink() != null && !videoPayload.youtubeLink().isBlank()) {
            return videoPayload.youtubeLink();
        }
        if (coursePayload.playlistId() != null && !coursePayload.playlistId().isBlank()) {
            return "https://www.youtube.com/playlist?list=" + coursePayload.playlistId() + "&index=" + (index + 1);
        }
        if (videoPayload.videoId() != null && !videoPayload.videoId().isBlank() && YOUTUBE_VIDEO_ID_PATTERN.matcher(videoPayload.videoId()).matches()) {
            return "https://www.youtube.com/watch?v=" + videoPayload.videoId();
        }
        return "https://www.youtube.com";
    }
}
