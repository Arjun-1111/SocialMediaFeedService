package com.project.SocialMediaFeedService.user.controller;

import com.project.SocialMediaFeedService.common.response.ApiResponse;
import com.project.SocialMediaFeedService.user.dto.request.RegisterRequest;
import com.project.SocialMediaFeedService.user.dto.request.UpdateUserRequest;
import com.project.SocialMediaFeedService.user.dto.response.UserResponse;
import com.project.SocialMediaFeedService.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest){
        UserResponse user = userService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User created successfully", user));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable String username){
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", user));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest updateUserRequest){
        UserResponse user = userService.updateUser(userId, updateUserRequest);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", user));
    }


}
