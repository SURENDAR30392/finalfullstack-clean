package com.fullstack.backend.controller;

import com.fullstack.backend.dto.InstructorStudentResponse;
import com.fullstack.backend.service.EnrollmentService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instructor")
@CrossOrigin(origins = "*")
public class InstructorStudentController {

    private final EnrollmentService enrollmentService;

    public InstructorStudentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/{instructorId}/students")
    public List<InstructorStudentResponse> getInstructorStudents(@PathVariable Long instructorId) {
        return enrollmentService.getInstructorStudents(instructorId);
    }
}
