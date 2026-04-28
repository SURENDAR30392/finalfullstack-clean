package com.fullstack.backend.config;

import com.fullstack.backend.dto.ImageUrlResponse;
import com.fullstack.backend.dto.PendingVideoApprovalResponse;
import com.fullstack.backend.dto.ProfileUrlResponse;
import com.fullstack.backend.dto.UserResponse;
import com.fullstack.backend.dto.VideoLinkResponse;
import com.fullstack.backend.dto.VideoResponse;
import com.fullstack.backend.entity.CourseVideo;
import com.fullstack.backend.entity.ImageUrl;
import com.fullstack.backend.entity.ProfileUrl;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.entity.VideoLink;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        Converter<User, UserResponse> userToUserResponse = context -> {
            User user = context.getSource();
            if (user == null) {
                return null;
            }
            return new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getProvider(),
                    user.isVerified(),
                    user.getCreatedAt()
            );
        };

        Converter<CourseVideo, VideoResponse> videoToVideoResponse = context -> {
            CourseVideo video = context.getSource();
            if (video == null) {
                return null;
            }
            return new VideoResponse(
                    video.getId(),
                    video.getTitle(),
                    video.getTopic(),
                    video.getYoutubeLink(),
                    video.getAssignmentUrl(),
                    video.getApprovalStatus()
            );
        };

        Converter<CourseVideo, PendingVideoApprovalResponse> videoToPendingApproval = context -> {
            CourseVideo video = context.getSource();
            if (video == null) {
                return null;
            }
            return new PendingVideoApprovalResponse(
                    video.getId(),
                    video.getCourse().getId(),
                    video.getCourse().getTitle(),
                    video.getCourse().getCreatedBy().getName(),
                    video.getTitle(),
                    video.getTopic(),
                    video.getYoutubeLink(),
                    video.getApprovalStatus()
            );
        };

        Converter<ImageUrl, ImageUrlResponse> imageToImageResponse = context -> {
            ImageUrl imageUrl = context.getSource();
            if (imageUrl == null) {
                return null;
            }
            return new ImageUrlResponse(
                    imageUrl.getId(),
                    imageUrl.getCourseId(),
                    imageUrl.getCourseName(),
                    imageUrl.getImageUrl(),
                    imageUrl.getUploadedById(),
                    imageUrl.getUploadedByName(),
                    imageUrl.getCreatedAt()
            );
        };

        Converter<ProfileUrl, ProfileUrlResponse> profileToProfileResponse = context -> {
            ProfileUrl profileUrl = context.getSource();
            if (profileUrl == null) {
                return null;
            }
            return new ProfileUrlResponse(
                    profileUrl.getId(),
                    profileUrl.getUserId(),
                    profileUrl.getUserName(),
                    profileUrl.getUserRole(),
                    profileUrl.getProfileUrl(),
                    profileUrl.getCreatedAt()
            );
        };

        Converter<VideoLink, VideoLinkResponse> videoLinkToVideoLinkResponse = context -> {
            VideoLink videoLink = context.getSource();
            if (videoLink == null) {
                return null;
            }
            return new VideoLinkResponse(
                    videoLink.getId(),
                    videoLink.getCourseId(),
                    videoLink.getVideoId(),
                    videoLink.getCourseName(),
                    videoLink.getTopicName(),
                    videoLink.getInstructorName(),
                    videoLink.getVideoTitle(),
                    videoLink.getCategory(),
                    videoLink.getYoutubeLink(),
                    videoLink.getCreatedAt()
            );
        };

        modelMapper.createTypeMap(User.class, UserResponse.class).setConverter(userToUserResponse);
        modelMapper.createTypeMap(CourseVideo.class, VideoResponse.class).setConverter(videoToVideoResponse);
        modelMapper.createTypeMap(CourseVideo.class, PendingVideoApprovalResponse.class).setConverter(videoToPendingApproval);
        modelMapper.createTypeMap(ImageUrl.class, ImageUrlResponse.class).setConverter(imageToImageResponse);
        modelMapper.createTypeMap(ProfileUrl.class, ProfileUrlResponse.class).setConverter(profileToProfileResponse);
        modelMapper.createTypeMap(VideoLink.class, VideoLinkResponse.class).setConverter(videoLinkToVideoLinkResponse);

        return modelMapper;
    }
}
