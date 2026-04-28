package com.fullstack.backend.controller;

import com.fullstack.backend.dto.CourseResponse;
import com.fullstack.backend.dto.LoadDataRequest;
import com.fullstack.backend.service.CourseService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/init")
@CrossOrigin(origins = "*")
public class InitController {

    private final CourseService courseService;

    public InitController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/load-data")
    public List<CourseResponse> loadData(@RequestBody LoadDataRequest request) {
        return courseService.loadFrontendData(request);
    }
}
