package com.fullstack.backend.controller;

import com.fullstack.backend.dto.LandingStatsResponse;
import com.fullstack.backend.service.LandingStatsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/landing-stats")
@CrossOrigin(origins = "*")
public class LandingStatsController {

    private final LandingStatsService landingStatsService;

    public LandingStatsController(LandingStatsService landingStatsService) {
        this.landingStatsService = landingStatsService;
    }

    @GetMapping
    public LandingStatsResponse getLandingStats() {
        return landingStatsService.getStats();
    }
}
