package com.fullstack.backend.controller;

import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.dto.CourseApprovalUpdateRequest;
import com.fullstack.backend.dto.CourseImageUrlUpdateRequest;
import com.fullstack.backend.dto.CourseRequest;
import com.fullstack.backend.dto.CourseResponse;
import com.fullstack.backend.dto.ImageUrlResponse;
import com.fullstack.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/api/courses")
    public List<CourseResponse> getCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/api/courses/published")
    public List<CourseResponse> getPublishedCourses() {
        return courseService.getApprovedCourses();
    }

    @GetMapping("/api/admin/content-approvals")
    public List<CourseResponse> getPendingContentApprovals() {
        return courseService.getPendingCourses();
    }

    @GetMapping("/api/image-urls")
    public List<ImageUrlResponse> getImageUrls() {
        return courseService.getAllImageUrls();
    }

    @PostMapping("/api/instructor/courses")
    public CourseResponse createCourse(@Valid @RequestBody CourseRequest request) {
        return courseService.createCourse(request);
    }

    @PutMapping("/api/instructor/courses/{id}")
    public CourseResponse updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.updateCourse(id, request);
    }

    @PutMapping("/api/courses/{id}/image-url")
    public CourseResponse updateCourseImageUrl(@PathVariable Long id, @Valid @RequestBody CourseImageUrlUpdateRequest request) {
        return courseService.updateCourseImageUrl(id, request);
    }

    @PutMapping("/api/admin/courses/{id}/approval")
    public CourseResponse updateCourseApproval(@PathVariable Long id, @Valid @RequestBody CourseApprovalUpdateRequest request) {
        return courseService.updateApprovalStatus(id, request);
    }

    @DeleteMapping("/api/instructor/courses/{id}")
    public ApiMessageResponse deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return new ApiMessageResponse("Course deleted successfully.");
    }

    @DeleteMapping("/api/instructor/course/{id}")
    public ApiMessageResponse deleteCourseLegacyPath(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return new ApiMessageResponse("Course deleted successfully.");
    }
}
