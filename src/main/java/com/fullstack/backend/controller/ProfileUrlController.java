package com.fullstack.backend.controller;

import com.fullstack.backend.dto.ProfileUrlRequest;
import com.fullstack.backend.dto.ProfileUrlResponse;
import com.fullstack.backend.service.ProfileUrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile-url")
@CrossOrigin(origins = "*")
public class ProfileUrlController {

    private final ProfileUrlService profileUrlService;

    public ProfileUrlController(ProfileUrlService profileUrlService) {
        this.profileUrlService = profileUrlService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileUrlResponse> getProfileUrl(@PathVariable Long userId) {
        ProfileUrlResponse response = profileUrlService.getLatestProfileUrl(userId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ProfileUrlResponse saveProfileUrl(@PathVariable Long userId, @Valid @RequestBody ProfileUrlRequest request) {
        return profileUrlService.saveProfileUrl(userId, request);
    }
}
