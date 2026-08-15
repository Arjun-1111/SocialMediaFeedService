package com.project.SocialMediaFeedService.feed.controller;

import com.project.SocialMediaFeedService.common.response.ApiResponse;
import com.project.SocialMediaFeedService.feed.dto.response.FeedResponse;
import com.project.SocialMediaFeedService.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feed")
public class FeedController {
    private final FeedService feedService;


    @GetMapping
    public ResponseEntity<ApiResponse<FeedResponse>> getFeed(@RequestParam(name = "userId") Long userId,
                                                             @RequestParam(name = "cursor", required = false) String cursor,
                                                             @RequestParam(name = "limit", required = false,defaultValue = "20") int limit
                                                             ){
        FeedResponse feed = feedService.getFeed(userId, cursor, limit);
        return ResponseEntity.ok().body(ApiResponse.success("User feed fetched Successfully !", feed));
    }

}
