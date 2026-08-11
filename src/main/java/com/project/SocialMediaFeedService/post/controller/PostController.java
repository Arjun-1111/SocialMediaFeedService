package com.project.SocialMediaFeedService.post.controller;

import com.project.SocialMediaFeedService.common.response.ApiResponse;
import com.project.SocialMediaFeedService.post.dto.request.CreatePostRequest;
import com.project.SocialMediaFeedService.post.dto.response.PostResponse;
import com.project.SocialMediaFeedService.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@RequestParam Long userId, @Valid @RequestBody CreatePostRequest request){
        PostResponse response = postService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Post Created Successfully", response));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long postId){
        PostResponse response = postService.getPostById(postId);
        return ResponseEntity.ok().body(ApiResponse.success("Post retrieved successfully", response));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getPostByUserId(@PathVariable Long userId){
        List<PostResponse> response = postService.getPostByUserId(userId);
        return ResponseEntity.ok().body(ApiResponse.success("Post retrieved Successfully", response));
    }
}
