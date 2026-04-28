package com.fullstack.backend.controller;

import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.service.EnrollmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "http://localhost:5173")
public class StudentController {

    private final EnrollmentService enrollmentService;

    public StudentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/enroll/{courseId}")
    public ApiMessageResponse enrollCourse(@PathVariable Long courseId, Authentication authentication) {
        return enrollmentService.enrollStudent(authentication.getName(), courseId);
    }
}
