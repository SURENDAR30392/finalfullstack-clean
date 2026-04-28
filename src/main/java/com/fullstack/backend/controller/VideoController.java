package com.fullstack.backend.controller;

import com.fullstack.backend.dto.CourseVideoRequest;
import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.dto.AssignmentLinkRequest;
import com.fullstack.backend.dto.PendingVideoApprovalResponse;
import com.fullstack.backend.dto.VideoApprovalUpdateRequest;
import com.fullstack.backend.dto.VideoResponse;
import com.fullstack.backend.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/{courseId}")
    public List<VideoResponse> getVideos(@PathVariable Long courseId) {
        return videoService.getVideosByCourseId(courseId);
    }

    @GetMapping("/admin/pending")
    public List<PendingVideoApprovalResponse> getPendingVideoApprovals() {
        return videoService.getPendingVideoApprovals();
    }

    @PostMapping
    public VideoResponse createVideo(@Valid @RequestBody CourseVideoRequest request) {
        return videoService.createVideo(request);
    }

    @PutMapping("/{videoId}")
    public VideoResponse updateVideo(@PathVariable Long videoId, @Valid @RequestBody CourseVideoRequest request) {
        return videoService.updateVideo(videoId, request);
    }

    @PutMapping("/{videoId}/assignment")
    public VideoResponse updateAssignment(@PathVariable Long videoId, @Valid @RequestBody AssignmentLinkRequest request) {
        return videoService.updateAssignmentUrl(videoId, request);
    }

    @PutMapping("/admin/{videoId}/approval")
    public VideoResponse updateVideoApproval(@PathVariable Long videoId, @Valid @RequestBody VideoApprovalUpdateRequest request) {
        return videoService.updateVideoApprovalStatus(videoId, request);
    }

    @DeleteMapping("/{videoId}")
    public ApiMessageResponse deleteVideo(@PathVariable Long videoId) {
        videoService.deleteVideo(videoId);
        return new ApiMessageResponse("Video deleted successfully.");
    }

}
