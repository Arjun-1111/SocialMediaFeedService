package com.project.SocialMediaFeedService.post.service.impl;

import com.project.SocialMediaFeedService.common.exception.ResourceNotFoundException;
import com.project.SocialMediaFeedService.post.dto.request.CreatePostRequest;
import com.project.SocialMediaFeedService.post.dto.response.PostResponse;
import com.project.SocialMediaFeedService.post.entity.Post;
import com.project.SocialMediaFeedService.post.mapper.PostMapper;
import com.project.SocialMediaFeedService.post.repository.PostRepository;
import com.project.SocialMediaFeedService.post.service.PostService;
import com.project.SocialMediaFeedService.user.entity.User;
import com.project.SocialMediaFeedService.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Long userId) {
        User fetchedUser = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user not found"));
        Post post = postMapper.toEntity(request, fetchedUser);
        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("postId not found"));
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getPostByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        List<Post> posts = postRepository.findByUserId(userId);
        return posts.stream().map(postMapper::toResponse).toList();
    }
}
