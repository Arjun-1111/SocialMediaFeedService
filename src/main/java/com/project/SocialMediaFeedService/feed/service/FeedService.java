package com.project.SocialMediaFeedService.feed.service;

import com.project.SocialMediaFeedService.feed.dto.response.FeedResponse;


public interface FeedService {
    FeedResponse getFeed(Long userId, String cursor, int limit);
}
