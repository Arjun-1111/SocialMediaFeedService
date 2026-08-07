package com.project.SocialMediaFeedService.user.repository;

import com.project.SocialMediaFeedService.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

//    atomic update methods for follow
//    The database performs the read and increment as one atomic operation — no race condition possible.
    @Modifying
    @Query("UPDATE user u set u.followerCount = u.followerCount + 1 where u.id = :userId")
    void incrementFollowerCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE user u set u.followerCount = u.followerCount - 1 where u.id = :userId and u.followerCount > 0")
    void decrementFollowerCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE user u set u.followingCount = u.followingCount + 1 where u.id = :userId")
    void incrementFollowingCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE user u set u.followingCount = u.followingCount -1 where u.id = :userId and u.followingCount > 0")
    void decrementFollowingCount(@Param("userId")  Long userId);


}

