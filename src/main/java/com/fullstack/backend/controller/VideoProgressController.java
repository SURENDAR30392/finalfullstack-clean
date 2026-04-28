package com.fullstack.backend.controller;

import com.fullstack.backend.dto.VideoProgressResponse;
import com.fullstack.backend.service.VideoProgressService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/{studentId}/courses/{courseId}/progress")
@CrossOrigin(origins = "*")
public class VideoProgressController {

    private final VideoProgressService videoProgressService;

    public VideoProgressController(VideoProgressService videoProgressService) {
        this.videoProgressService = videoProgressService;
    }

    @GetMapping
    public VideoProgressResponse getCourseProgress(@PathVariable Long studentId, @PathVariable Long courseId) {
        return videoProgressService.getCourseProgress(studentId, courseId);
    }

    @PostMapping("/watch/{videoId}")
    public VideoProgressResponse setLastWatchedVideo(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            @PathVariable Long videoId
    ) {
        return videoProgressService.setLastWatchedVideo(studentId, courseId, videoId);
    }

    @PostMapping("/complete/{videoId}")
    public VideoProgressResponse markVideoComplete(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            @PathVariable Long videoId
    ) {
        return videoProgressService.markVideoComplete(studentId, courseId, videoId);
    }
}
