package com.project.SocialMediaFeedService.user.service;

import com.project.SocialMediaFeedService.user.dto.request.RegisterRequest;
import com.project.SocialMediaFeedService.user.dto.request.UpdateUserRequest;
import com.project.SocialMediaFeedService.user.dto.response.UserResponse;

public interface UserService {

    UserResponse registerUser(RegisterRequest registerRequest);
    UserResponse getUserByUsername(String username);
    UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest);
}
