package com.project.SocialMediaFeedService.feed.service.impl;

import com.project.SocialMediaFeedService.common.exception.ResourceNotFoundException;
import com.project.SocialMediaFeedService.feed.repository.FeedRedisRepository;
import com.project.SocialMediaFeedService.feed.service.FanoutService;
import com.project.SocialMediaFeedService.follow.repository.FollowRepository;
import com.project.SocialMediaFeedService.user.entity.User;
import com.project.SocialMediaFeedService.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FanoutServiceImpl implements FanoutService {
    private final FollowRepository followRepository;
    private final FeedRedisRepository feedRedisRepository;
    private final UserRepository userRepository;

    private static final long CELEBRITY_THRESHOLD = 10_000L;


    @Override
    @Async("backgroundTaskExecutor")
    //@Async must be on the implementation method, not the interface.
    public void fanoutToFollowers(Long postId, Long authorId, double score) {
        //check user Follower Count
        User user = userRepository.findById(authorId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long followerCount = user.getFollowerCount();

        //If user followerCount greater than 10_000 skip fanout - Their posts will be fetched at read time
        if(followerCount >= CELEBRITY_THRESHOLD){
            log.info("Skipping fanout for celebrity authorId={}", authorId);
            return;
        }

        //get all follower ID
        List<Long> followerIds = followRepository.findFollowerIdsByUserId(authorId);

        //If no followers, return early
        if(followerIds.isEmpty()) {
            return;
        }


        log.info("Starting fanout for postId={} authorId={} followerCount={}",
                postId, authorId, followerIds.size());

        //For each follower, add to their feed
        for(Long followerId : followerIds){
            feedRedisRepository.addToFeed(followerId,postId,score);
        }

        log.info("Completed fanout for postId={}", postId);

    }
}
