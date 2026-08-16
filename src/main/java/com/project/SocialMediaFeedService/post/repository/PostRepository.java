package com.project.SocialMediaFeedService.post.repository;

import com.project.SocialMediaFeedService.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);

    //Rebuild feed from PostgresSQL (when Redis is empty)
    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds ORDER BY p.createdAt DESC LIMIT :limit")
    List<Post> findRecentPostsByUserIds(@Param("userIds") List<Long> userIds, @Param("limit") int limit);

    //Get posts before cursor (for pagination)
    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds AND p.createdAt < :before ORDER BY p.createdAt DESC LIMIT :limit")
    List<Post> findRecentPostsByUserIdsAndBefore(@Param("userIds") List<Long> userIds, @Param("before")LocalDateTime before, @Param("limit") int limit );
}
