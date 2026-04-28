package com.fullstack.backend.service;

import com.fullstack.backend.dto.VideoLinkResponse;
import com.fullstack.backend.entity.Course;
import com.fullstack.backend.entity.CourseVideo;
import com.fullstack.backend.entity.VideoLink;
import com.fullstack.backend.repository.CourseRepository;
import com.fullstack.backend.repository.VideoLinkRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoLinkService {

    private final VideoLinkRepository videoLinkRepository;
    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;

    public VideoLinkService(VideoLinkRepository videoLinkRepository, CourseRepository courseRepository, ModelMapper modelMapper) {
        this.videoLinkRepository = videoLinkRepository;
        this.courseRepository = courseRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public void syncCourseVideoLinks(Course course) {
        Course managedCourse = courseRepository.findById(course.getId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + course.getId()));

        videoLinkRepository.deleteByCourseId(managedCourse.getId());

        List<VideoLink> links = managedCourse.getVideos()
                .stream()
                .map(video -> buildVideoLink(managedCourse, video))
                .toList();

        videoLinkRepository.saveAll(links);
    }

    @Transactional
    public void deleteByCourseId(Long courseId) {
        videoLinkRepository.deleteByCourseId(courseId);
    }

    @Transactional
    public void syncAllFromCourses() {
        for (Course course : courseRepository.findAll()) {
            syncCourseVideoLinks(course);
        }
    }

    public List<VideoLinkResponse> getAllVideoLinks() {
        return videoLinkRepository.findAllByOrderByCourseNameAscTopicNameAsc()
                .stream()
                .map(videoLink -> modelMapper.map(videoLink, VideoLinkResponse.class))
                .toList();
    }

    private VideoLink buildVideoLink(Course course, CourseVideo video) {
        VideoLink videoLink = new VideoLink();
        videoLink.setCourseId(course.getId());
        videoLink.setVideoId(video.getId());
        videoLink.setCourseName(course.getTitle());
        videoLink.setTopicName(video.getTopic());
        videoLink.setInstructorName(course.getCreatedBy().getName());
        videoLink.setVideoTitle(video.getTitle());
        videoLink.setCategory(course.getCategory());
        videoLink.setYoutubeLink(video.getYoutubeLink());
        return videoLink;
    }
}
