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
//    Java entity class is named User with a capital U.
//    JPQL is case sensitive for entity names.
//    Important: because of Hibernate's first-level cache, add @Modifying(clearAutomatically = true) to your @Modifying queries in UserRepository — this clears the cache after each update so fresh data is returned
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.followerCount = u.followerCount + 1 WHERE u.id = :userId")
    void incrementFollowerCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.followerCount = u.followerCount - 1 WHERE u.id = :userId AND u.followerCount > 0")
    void decrementFollowerCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.followingCount = u.followingCount + 1 WHERE u.id = :userId")
    void incrementFollowingCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.followingCount = u.followingCount -1 WHERE u.id = :userId AND u.followingCount > 0")
    void decrementFollowingCount(@Param("userId")  Long userId);


}

