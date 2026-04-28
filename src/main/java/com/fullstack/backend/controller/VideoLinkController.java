package com.fullstack.backend.controller;

import com.fullstack.backend.dto.VideoLinkResponse;
import com.fullstack.backend.service.VideoLinkService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video-links")
@CrossOrigin(origins = "*")
public class VideoLinkController {

    private final VideoLinkService videoLinkService;

    public VideoLinkController(VideoLinkService videoLinkService) {
        this.videoLinkService = videoLinkService;
    }

    @GetMapping
    public List<VideoLinkResponse> getAllVideoLinks() {
        return videoLinkService.getAllVideoLinks();
    }
}
