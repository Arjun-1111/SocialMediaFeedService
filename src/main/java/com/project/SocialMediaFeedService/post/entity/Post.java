package com.project.SocialMediaFeedService.post.entity;

import com.project.SocialMediaFeedService.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "posts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class  Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 3000, nullable = false)
    String content;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(nullable = false, length = 50)
    String username;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Builder.Default
    @Column(name = "like_count", nullable = false,columnDefinition = "bigint default 0")
    Long likeCount = 0L;

    @Builder.Default
    @Column(name = "comment_count", nullable = false, columnDefinition = "bigint default 0")
    Long commentCount = 0L;
}
