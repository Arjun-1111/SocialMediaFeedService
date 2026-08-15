package com.project.SocialMediaFeedService.feed.service;

import org.springframework.scheduling.annotation.Async;

public interface FanoutService {
    void fanoutToFollowers(Long postId, Long authorId, double score);

}
