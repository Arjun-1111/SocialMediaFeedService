package com.project.SocialMediaFeedService.follow.dto.response;

import lombok.*;

import java.time.LocalDateTime;

//followerId	Who did the following
//followingId	Who was followed
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
