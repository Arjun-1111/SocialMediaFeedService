package com.project.SocialMediaFeedService.user.service.impl;

import com.project.SocialMediaFeedService.common.exception.DuplicateResourceException;
import com.project.SocialMediaFeedService.common.exception.ResourceNotFoundException;
import com.project.SocialMediaFeedService.user.dto.request.RegisterRequest;
import com.project.SocialMediaFeedService.user.dto.request.UpdateUserRequest;
import com.project.SocialMediaFeedService.user.dto.response.UserResponse;
import com.project.SocialMediaFeedService.user.entity.User;
import com.project.SocialMediaFeedService.user.mapper.UserMapper;
import com.project.SocialMediaFeedService.user.repository.UserRepository;
import com.project.SocialMediaFeedService.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest registerRequest) {
        //check username exists
        if (isUsernameExists(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        //check email exists
        if (userRepository.existsByEmail((registerRequest.getEmail()))) {
            throw new DuplicateResourceException("Email already exists");
        }

        //hash password
        String hashedPassword = hashPassword(registerRequest.getPassword());

        //userMapper to entity
        User user = userMapper.toEntity(registerRequest);

        //set hashed password
        user.setPassword(hashedPassword);

        //save user to database
        User saveduser = userRepository.save(user);

        //userMapper to response and return
        return userMapper.toResponse(saveduser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " not found"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        //check if user with id exists, if not throw exception
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        if (updateUserRequest.getUsername() != null) {
            user.setUsername(updateUserRequest.getUsername());
        }
        if (updateUserRequest.getBio() != null) {
            user.setBio(updateUserRequest.getBio());
        }
        if (updateUserRequest.getAvatarUrl() != null) {
            user.setAvatarUrl(updateUserRequest.getAvatarUrl());
        }

        //save user to database
        User updatedUser = userRepository.save(user);

        //return userMapper to response and return
        return userMapper.toResponse(updatedUser);
    }

    private boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

}
