package com.project.SocialMediaFeedService.user.dto.response;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;

    private String username;

    private String bio;

    private String avatarUrl;

    private Long followerCount;

    private Long followingCount;

    private LocalDateTime createdAt;
}
