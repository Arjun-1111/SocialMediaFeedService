package com.project.SocialMediaFeedService.user.entity;

import com.project.SocialMediaFeedService.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 50)
    String username;

    @Column(nullable = false,unique = true, length = 100)
    String email;

    @Column(nullable = false, length = 100)
    String password;

    @Column(length = 500)
    String bio;

    @Column(name= "avatar_url")
    String avatarUrl;

    @Column(name = "follower_count", nullable = false, columnDefinition = "bigint default 0")
    Long followerCount = 0L;

    @Column(name = "following_count", nullable = false, columnDefinition = "bigint default 0")
    Long followingCount = 0L;

}
