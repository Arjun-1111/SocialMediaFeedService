package com.project.SocialMediaFeedService.post.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostResponse {

    private Long id;

    private String content;

    private Long userId;

    private String username;

    private String avatarUrl;

    private Long likeCount;

    private Long commentCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
