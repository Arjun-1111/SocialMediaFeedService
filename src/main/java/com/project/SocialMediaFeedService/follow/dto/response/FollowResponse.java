package com.project.SocialMediaFeedService.follow.dto.response;

import lombok.*;

import java.time.LocalDateTime;
//Eg: user A followed user B
//followerId	Who did the following - UserA
//followingId	Who was followed -User B
//followerCount	User B's updated follower count
//followingCount	User A's updated following count
//followedAt	When it happened

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowResponse {
    private Long followerId;
    private Long followingId;
    private Long followerCount;
    private Long followingCount;
    private LocalDateTime followedAt;
}
