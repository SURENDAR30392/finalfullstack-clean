package com.fullstack.backend.service;

import com.fullstack.backend.dto.CourseVideoRequest;
import com.fullstack.backend.dto.AssignmentLinkRequest;
import com.fullstack.backend.dto.PendingVideoApprovalResponse;
import com.fullstack.backend.dto.VideoApprovalUpdateRequest;
import com.fullstack.backend.dto.VideoResponse;
import com.fullstack.backend.entity.AssignmentLink;
import com.fullstack.backend.entity.Course;
import com.fullstack.backend.entity.CourseVideo;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.exception.BadRequestException;
import com.fullstack.backend.exception.ResourceNotFoundException;
import com.fullstack.backend.repository.AssignmentLinkRepository;
import com.fullstack.backend.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final VideoLinkService videoLinkService;
    private final AssignmentLinkRepository assignmentLinkRepository;
    private final VideoProgressService videoProgressService;
    private final ModelMapper modelMapper;

    public VideoService(
            VideoRepository videoRepository,
            CourseService courseService,
            UserService userService,
            VideoLinkService videoLinkService,
            AssignmentLinkRepository assignmentLinkRepository,
            VideoProgressService videoProgressService,
            ModelMapper modelMapper
    ) {
        this.videoRepository = videoRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.videoLinkService = videoLinkService;
        this.assignmentLinkRepository = assignmentLinkRepository;
        this.videoProgressService = videoProgressService;
        this.modelMapper = modelMapper;
    }

    public List<VideoResponse> getVideosByCourseId(Long courseId) {
        courseService.getCourseEntity(courseId);
        return videoRepository.findByCourseIdOrderByIdAsc(courseId)
                .stream()
                .map(video -> modelMapper.map(video, VideoResponse.class))
                .toList();
    }

    @Transactional
    public VideoResponse createVideo(CourseVideoRequest request) {
        Course course = courseService.getCourseEntity(request.courseId());

        CourseVideo video = modelMapper.map(request, CourseVideo.class);
        video.setTitle(resolveVideoTitle(request));
        video.setTopic(request.topic());
        video.setYoutubeLink(request.youtubeLink());
        video.setAssignmentUrl(normalizeAssignmentUrl(request.assignmentUrl()));
        video.setApprovalStatus("PENDING");
        video.setCourse(course);

        CourseVideo savedVideo = videoRepository.save(video);
        saveAssignmentLinkIfPresent(course, savedVideo);
        videoLinkService.syncCourseVideoLinks(courseService.getCourseEntity(request.courseId()));
        return modelMapper.map(savedVideo, VideoResponse.class);
    }

    @Transactional
    public VideoResponse updateVideo(Long videoId, CourseVideoRequest request) {
        CourseVideo video = getVideoEntity(videoId);
        Course course = courseService.getCourseEntity(request.courseId());

        video.setTitle(resolveVideoTitle(request));
        video.setTopic(request.topic());
        video.setYoutubeLink(request.youtubeLink());
        video.setAssignmentUrl(normalizeAssignmentUrl(request.assignmentUrl()));
        video.setApprovalStatus("PENDING");
        video.setCourse(course);

        CourseVideo savedVideo = videoRepository.save(video);
        saveAssignmentLinkIfPresent(course, savedVideo);
        videoLinkService.syncCourseVideoLinks(course);
        return modelMapper.map(savedVideo, VideoResponse.class);
    }

    @Transactional
    public VideoResponse updateAssignmentUrl(Long videoId, AssignmentLinkRequest request) {
        CourseVideo video = getVideoEntity(videoId);
        Course course = video.getCourse();

        String assignmentUrl = normalizeAssignmentUrl(request.assignmentUrl());
        if (assignmentUrl == null) {
            throw new BadRequestException("Assignment link is required.");
        }

        video.setAssignmentUrl(assignmentUrl);
        CourseVideo savedVideo = videoRepository.save(video);
        saveAssignmentLinkIfPresent(course, savedVideo);
        return modelMapper.map(savedVideo, VideoResponse.class);
    }

    @Transactional
    public void deleteVideo(Long videoId) {
        CourseVideo video = getVideoEntity(videoId);
        Long courseId = video.getCourse().getId();
        assignmentLinkRepository.deleteByVideoId(videoId);
        videoRepository.delete(video);
        videoProgressService.refreshCourseProgressAfterVideoDeletion(courseId, videoId);
        videoLinkService.syncCourseVideoLinks(courseService.getCourseEntity(courseId));
    }

    @Transactional
    public List<PendingVideoApprovalResponse> getPendingVideoApprovals() {
        return videoRepository.findByApprovalStatusOrderByIdDesc("PENDING")
                .stream()
                .map(video -> modelMapper.map(video, PendingVideoApprovalResponse.class))
                .toList();
    }

    @Transactional
    public VideoResponse updateVideoApprovalStatus(Long videoId, VideoApprovalUpdateRequest request) {
        CourseVideo video = getVideoEntity(videoId);
        User admin = userService.getUserEntity(request.updatedBy());
        validateAdminRole(admin);

        String normalizedStatus = request.status().trim().toUpperCase();
        if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            throw new BadRequestException("Video approval status must be APPROVED or REJECTED.");
        }

        video.setApprovalStatus(normalizedStatus);
        return modelMapper.map(videoRepository.save(video), VideoResponse.class);
    }

    public CourseVideo getVideoEntity(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + videoId));
    }

    private void validateAdminRole(User user) {
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        if (!"ADMIN".equals(role)) {
            throw new BadRequestException("Only admin can approve or reject videos.");
        }
    }

    private String normalizeAssignmentUrl(String assignmentUrl) {
        if (assignmentUrl == null) {
            return null;
        }
        String trimmed = assignmentUrl.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String resolveVideoTitle(CourseVideoRequest request) {
        String title = request.title();
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }

        String youtubeLink = request.youtubeLink() == null ? "" : request.youtubeLink().trim();
        if (!youtubeLink.isEmpty()) {
            return "Video - " + youtubeLink;
        }

        String assignmentUrl = normalizeAssignmentUrl(request.assignmentUrl());
        if (assignmentUrl != null) {
            return extractFileName(assignmentUrl);
        }

        return "Untitled Video";
    }

    private String extractFileName(String assignmentUrl) {
        int slashIndex = Math.max(assignmentUrl.lastIndexOf('/'), assignmentUrl.lastIndexOf('\\'));
        String fileName = slashIndex >= 0 ? assignmentUrl.substring(slashIndex + 1) : assignmentUrl;
        return fileName == null || fileName.isBlank() ? assignmentUrl : fileName;
    }

    private void saveAssignmentLinkIfPresent(Course course, CourseVideo video) {
        String assignmentUrl = video.getAssignmentUrl();
        if (assignmentUrl == null || assignmentUrl.isBlank()) {
            return;
        }

        AssignmentLink link = new AssignmentLink();
        link.setCourseId(course.getId());
        link.setCourseName(course.getTitle());
        link.setVideoId(video.getId());
        link.setVideoTitle(video.getTitle());
        link.setAssignmentUrl(assignmentUrl);
        link.setInstructorId(course.getCreatedBy().getId());
        link.setInstructorName(course.getCreatedBy().getName());
        assignmentLinkRepository.save(link);
    }
}
