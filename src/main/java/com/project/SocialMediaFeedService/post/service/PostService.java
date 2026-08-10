package com.project.SocialMediaFeedService.post.service;

import com.project.SocialMediaFeedService.post.dto.request.CreatePostRequest;
import com.project.SocialMediaFeedService.post.dto.response.PostResponse;

import java.util.List;

public interface PostService {
    PostResponse createPost(CreatePostRequest request, Long userId);
    PostResponse getPostById(Long postId);
    List<PostResponse> getPostByUserId(Long userId);
}
