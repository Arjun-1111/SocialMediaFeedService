package com.project.SocialMediaFeedService.follow.repository;

import com.project.SocialMediaFeedService.follow.entity.Follow;
import com.project.SocialMediaFeedService.follow.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    @Query("select f.id.followerId from Follow f where f.id.followingId  = :userId")
    List<Long> findFollowerIdsByUserId(@Param("userId") Long userId);

    @Query("select f.id.followingId from Follow f where f.id.followerId = :userId")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);
}
