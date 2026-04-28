package com.fullstack.backend.service;

import com.fullstack.backend.dto.VideoProgressResponse;
import com.fullstack.backend.entity.Course;
import com.fullstack.backend.entity.CourseVideo;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.entity.VideoProgress;
import com.fullstack.backend.exception.BadRequestException;
import com.fullstack.backend.exception.ResourceNotFoundException;
import com.fullstack.backend.repository.EnrollmentRepository;
import com.fullstack.backend.repository.VideoProgressRepository;
import com.fullstack.backend.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VideoProgressService {

    private final VideoProgressRepository videoProgressRepository;
    private final VideoRepository videoRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final CourseService courseService;

    public VideoProgressService(
            VideoProgressRepository videoProgressRepository,
            VideoRepository videoRepository,
            EnrollmentRepository enrollmentRepository,
            UserService userService,
            CourseService courseService
    ) {
        this.videoProgressRepository = videoProgressRepository;
        this.videoRepository = videoRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userService = userService;
        this.courseService = courseService;
    }

    @Transactional
    public VideoProgressResponse getCourseProgress(Long userId, Long courseId) {
        User user = userService.getUserEntity(userId);
        Course course = courseService.getCourseEntity(courseId);
        validateEnrolled(userId, courseId);

        VideoProgress summary = getOrCreateCourseSummary(user, course);
        return buildResponse(summary, course);
    }

    @Transactional
    public VideoProgressResponse markVideoComplete(Long userId, Long courseId, Long videoId) {
        User user = userService.getUserEntity(userId);
        Course course = courseService.getCourseEntity(courseId);
        validateEnrolled(userId, courseId);
        CourseVideo video = getCourseVideo(videoId, courseId);

        VideoProgress summary = getOrCreateCourseSummary(user, course);
        Set<Long> completedVideoIds = new LinkedHashSet<>(parseCompletedVideoIds(summary.getCompletedVideoIdsData()));
        completedVideoIds.add(video.getId());

        applySummaryFields(summary, user, course, video, completedVideoIds);
        videoProgressRepository.save(summary);

        return buildResponse(summary, course);
    }

    @Transactional
    public VideoProgressResponse setLastWatchedVideo(Long userId, Long courseId, Long videoId) {
        User user = userService.getUserEntity(userId);
        Course course = courseService.getCourseEntity(courseId);
        validateEnrolled(userId, courseId);
        CourseVideo video = getCourseVideo(videoId, courseId);

        VideoProgress summary = getOrCreateCourseSummary(user, course);
        Set<Long> completedVideoIds = new LinkedHashSet<>(parseCompletedVideoIds(summary.getCompletedVideoIdsData()));

        applySummaryFields(summary, user, course, video, completedVideoIds);
        videoProgressRepository.save(summary);

        return buildResponse(summary, course);
    }

    @Transactional
    public void backfillExistingProgressSummaries() {
        Map<String, List<VideoProgress>> groupedRows = new HashMap<>();

        for (VideoProgress row : videoProgressRepository.findAll()) {
            if (row.getUser() == null || row.getCourse() == null) {
                continue;
            }
            String key = row.getUser().getId() + ":" + row.getCourse().getId();
            groupedRows.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }

        for (List<VideoProgress> rows : groupedRows.values()) {
            VideoProgress latest = rows.stream()
                    .max(Comparator.comparing(VideoProgress::getLastUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);

            if (latest == null) {
                continue;
            }

            User user = latest.getUser();
            Course course = latest.getCourse();

            Set<Long> completedVideoIds = new LinkedHashSet<>();
            for (VideoProgress row : rows) {
                completedVideoIds.addAll(parseCompletedVideoIds(row.getCompletedVideoIdsData()));
                if ((Boolean.TRUE.equals(row.getCompleted()) || (row.getProgressPercent() != null && row.getProgressPercent() >= 100))
                        && row.getVideo() != null
                        && row.getVideo().getId() != null) {
                    completedVideoIds.add(row.getVideo().getId());
                }
            }

            applySummaryFields(latest, user, course, latest.getVideo(), completedVideoIds);
            videoProgressRepository.save(latest);

            List<VideoProgress> duplicates = rows.stream()
                    .filter(row -> !row.getId().equals(latest.getId()))
                    .toList();

            if (!duplicates.isEmpty()) {
                videoProgressRepository.deleteAll(duplicates);
            }
        }
    }

    @Transactional
    public void refreshCourseProgressAfterVideoDeletion(Long courseId, Long deletedVideoId) {
        Course course = courseService.getCourseEntity(courseId);
        List<VideoProgress> progressRows = videoProgressRepository.findByCourseId(courseId);

        for (VideoProgress row : progressRows) {
            if (row.getUser() == null) {
                continue;
            }

            Set<Long> completedVideoIds = new LinkedHashSet<>(parseCompletedVideoIds(row.getCompletedVideoIdsData()));
            completedVideoIds.remove(deletedVideoId);

            CourseVideo lastWatchedVideo = row.getVideo();
            if (lastWatchedVideo != null && deletedVideoId.equals(lastWatchedVideo.getId())) {
                lastWatchedVideo = course.getVideos().stream().findFirst().orElse(null);
            }

            applySummaryFields(row, row.getUser(), course, lastWatchedVideo, completedVideoIds);
            videoProgressRepository.save(row);
        }
    }

    private void validateEnrolled(Long userId, Long courseId) {
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BadRequestException("Student is not enrolled in this course.");
        }
    }

    private CourseVideo getCourseVideo(Long videoId, Long courseId) {
        CourseVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + videoId));

        if (!video.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Video does not belong to the selected course.");
        }

        return video;
    }

    private VideoProgress getOrCreateCourseSummary(User user, Course course) {
        List<VideoProgress> existingRows = videoProgressRepository.findAllByUserIdAndCourseId(user.getId(), course.getId());
        if (existingRows.isEmpty()) {
            VideoProgress summary = new VideoProgress();
            summary.setUser(user);
            summary.setCourse(course);
            applySummaryFields(summary, user, course, course.getVideos().stream().findFirst().orElse(null), Set.of());
            return summary;
        }

        VideoProgress summary = existingRows.stream()
                .max(Comparator.comparing(VideoProgress::getLastUpdatedAt))
                .orElse(existingRows.get(0));

        Set<Long> completedVideoIds = existingRows.stream()
                .flatMap(row -> parseCompletedVideoIds(row.getCompletedVideoIdsData()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        CourseVideo lastWatchedVideo = summary.getVideo();
        applySummaryFields(summary, user, course, lastWatchedVideo, completedVideoIds);

        List<VideoProgress> duplicates = existingRows.stream()
                .filter(row -> !row.getId().equals(summary.getId()))
                .toList();

        if (!duplicates.isEmpty()) {
            videoProgressRepository.deleteAll(duplicates);
        }

        return summary;
    }

    private void applySummaryFields(
            VideoProgress summary,
            User user,
            Course course,
            CourseVideo lastWatchedVideo,
            Set<Long> completedVideoIds
    ) {
        List<CourseVideo> orderedVideos = videoRepository.findByCourseIdOrderByIdAsc(course.getId());
        Set<Long> validVideoIds = orderedVideos.stream().map(CourseVideo::getId).collect(Collectors.toSet());
        List<Long> filteredCompletedIds = completedVideoIds.stream()
                .filter(validVideoIds::contains)
                .sorted()
                .toList();

        int totalVideos = orderedVideos.size();
        int completedVideos = filteredCompletedIds.size();
        int progressPercent = totalVideos > 0 ? (int) Math.round((completedVideos * 100.0) / totalVideos) : 0;

        summary.setUser(user);
        summary.setCourse(course);
        summary.setVideo(lastWatchedVideo);
        summary.setStudentName(user.getName());
        summary.setInstructorName(course.getCreatedBy() != null ? course.getCreatedBy().getName() : "");
        summary.setCourseName(course.getTitle());
        summary.setEnrolledCoursesCount(enrollmentRepository.findByUserId(user.getId()).size());
        summary.setTotalVideosInCourse(totalVideos);
        summary.setCompletedVideosInCourse(completedVideos);
        summary.setCompletedVideoIdsData(filteredCompletedIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        summary.setProgressPercent(progressPercent);
        summary.setCompleted(totalVideos > 0 && completedVideos == totalVideos);
    }

    private VideoProgressResponse buildResponse(VideoProgress summary, Course course) {
        List<CourseVideo> orderedVideos = videoRepository.findByCourseIdOrderByIdAsc(course.getId());
        List<Long> completedVideoIds = parseCompletedVideoIds(summary.getCompletedVideoIdsData());

        return new VideoProgressResponse(
                course.getId(),
                summary.getTotalVideosInCourse() != null ? summary.getTotalVideosInCourse() : orderedVideos.size(),
                summary.getCompletedVideosInCourse() != null ? summary.getCompletedVideosInCourse() : completedVideoIds.size(),
                summary.getProgressPercent() != null ? summary.getProgressPercent() : 0,
                completedVideoIds,
                summary.getVideo() != null ? summary.getVideo().getId() : orderedVideos.stream().findFirst().map(CourseVideo::getId).orElse(null),
                summary.getVideo() != null ? summary.getVideo().getTopic() : orderedVideos.stream().findFirst().map(CourseVideo::getTopic).orElse("")
        );
    }

    private List<Long> parseCompletedVideoIds(String completedVideoIdsData) {
        if (completedVideoIdsData == null || completedVideoIdsData.isBlank()) {
            return new ArrayList<>();
        }

        return List.of(completedVideoIdsData.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .distinct()
                .sorted()
                .toList();
    }
}
