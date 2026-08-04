package com.project.SocialMediaFeedService.user.mapper;

import com.project.SocialMediaFeedService.user.dto.request.RegisterRequest;
import com.project.SocialMediaFeedService.user.dto.response.UserResponse;
import com.project.SocialMediaFeedService.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request){
    return User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(request.getPassword())
            .build();
    }

    public UserResponse toResponse(User user){
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
