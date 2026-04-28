package com.fullstack.backend.controller;

import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.service.VideoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class InstructorDeleteController {

    private final VideoService videoService;

    public InstructorDeleteController(VideoService videoService) {
        this.videoService = videoService;
    }

    @DeleteMapping("/api/instructor/video/{videoId}")
    public ApiMessageResponse deleteVideo(@PathVariable Long videoId) {
        videoService.deleteVideo(videoId);
        return new ApiMessageResponse("Video deleted successfully.");
    }
}
