package com.project.SocialMediaFeedService.follow.service;

import com.project.SocialMediaFeedService.follow.dto.response.FollowResponse;

public interface FollowService {
    FollowResponse follow(Long followerId, Long followingId);
    void unfollow(Long followerId, Long followingId);
}
