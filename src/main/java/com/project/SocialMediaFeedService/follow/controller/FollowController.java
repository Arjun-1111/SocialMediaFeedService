package com.project.SocialMediaFeedService.follow.controller;

import com.project.SocialMediaFeedService.common.response.ApiResponse;
import com.project.SocialMediaFeedService.follow.dto.response.FollowResponse;
import com.project.SocialMediaFeedService.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/follows")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{followingId}")
    public ResponseEntity<ApiResponse<FollowResponse>> follow(@PathVariable Long followingId, @RequestParam Long followerId){
         return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User followed Successfully", followService.follow(followerId,followingId)));
    }

    @DeleteMapping("/{followingId}")
    public ResponseEntity<ApiResponse<?>> unfollow(@PathVariable Long followingId, @RequestParam Long followerId){
        followService.unfollow(followerId,followingId);
        return ResponseEntity.ok().body(ApiResponse.success("User Unfollowed Successfully"));
    }


}
