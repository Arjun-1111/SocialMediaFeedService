package com.project.SocialMediaFeedService.follow.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowResponse {
    private Long followerId;
    private Long followingId;
    private Long followerCount;
    private LocalDateTime followedAt;
}
