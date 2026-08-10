package com.project.SocialMediaFeedService.post.mapper;

import com.project.SocialMediaFeedService.post.dto.request.CreatePostRequest;
import com.project.SocialMediaFeedService.post.dto.response.PostResponse;
import com.project.SocialMediaFeedService.post.entity.Post;
import com.project.SocialMediaFeedService.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public Post toEntity(CreatePostRequest request, User user){
        return Post.builder()
                .content(request.getContent())
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public PostResponse toResponse(Post post){
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .userId(post.getUserId())
                .username(post.getUsername())
                .avatarUrl(post.getAvatarUrl())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
