package com.fullstack.backend.service;

import com.fullstack.backend.dto.ProfileUrlRequest;
import com.fullstack.backend.dto.ProfileUrlResponse;
import com.fullstack.backend.entity.ProfileUrl;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.exception.BadRequestException;
import com.fullstack.backend.repository.ProfileUrlRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class ProfileUrlService {

    private final ProfileUrlRepository profileUrlRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    public ProfileUrlService(ProfileUrlRepository profileUrlRepository, UserService userService, ModelMapper modelMapper) {
        this.profileUrlRepository = profileUrlRepository;
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    public ProfileUrlResponse getLatestProfileUrl(Long userId) {
        return profileUrlRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(userId)
                .map(profileUrl -> modelMapper.map(profileUrl, ProfileUrlResponse.class))
                .orElse(null);
    }

    public ProfileUrlResponse saveProfileUrl(Long userId, ProfileUrlRequest request) {
        String normalizedUrl = normalizeProfileUrl(request.profileUrl());
        if (normalizedUrl == null) {
            throw new BadRequestException("Profile URL is required.");
        }

        User user = userService.getUserEntity(userId);

        ProfileUrl profileUrl = new ProfileUrl();
        profileUrl.setUserId(user.getId());
        profileUrl.setUserName(user.getName());
        profileUrl.setUserRole(user.getRole());
        profileUrl.setProfileUrl(normalizedUrl);

        return modelMapper.map(profileUrlRepository.save(profileUrl), ProfileUrlResponse.class);
    }

    private String normalizeProfileUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
