package com.project.SocialMediaFeedService.feed.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class FeedRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String FEED_KEY_PREFIX = "feed:user:";
    private static final String POST_CACHE_KEY_PREFIX = "post:cache:";
    private static final long FEED_TTL_MINUTES = 5;
    private static final long POST_CACHE_TTL_MINUTES = 5;

    //Add post to a user's feed
    public void addToFeed(Long userId, Long postId, double score){
        String key = feedKey(userId);
        redisTemplate.opsForZSet().add(key, postId.toString(), score);
        //After adding, reset TTL
        redisTemplate.expire(key, Duration.ofMinutes(FEED_TTL_MINUTES));
    }

    //Get posts from feed with cursor
    //reverseRangeByScore(
    //    String key,       // which sorted set
    //    double min,       // minimum score (inclusive)
    //    double max,       // maximum score (inclusive)
    //    long offset,      // how many items to skip
    //    long count        // how many items to return
    //)
    public Set<String> getFeedPostIds(Long userId, double maxScore, int limit){
        String key = feedKey(userId);
        return redisTemplate.opsForZSet().reverseRangeByScore(key, 0, maxScore, 0, limit);
    }

    //Remove post from feed
    public void removeFromFeed(Long userId, Long postId){
        String key = feedKey(userId);
        redisTemplate.opsForZSet().remove(key, postId.toString());
    }

    //Cache a post
    public void cachePost(Long postId, String postJson){
        String key = postCacheKey(postId);
        redisTemplate.opsForValue().set(key, postJson, Duration.ofMinutes(POST_CACHE_TTL_MINUTES));
    }

    //Get cached post
    public Optional<String> getCachedPost(Long postId){
        String key = postCacheKey(postId);
      return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    //Delete cached post
    public void deleteCachedPost(Long postId){
        String key = postCacheKey(postId);
        redisTemplate.delete(key);
    }

    //Check if feed exists
    public boolean feedExists(Long userId){
        String key = feedKey(userId);
        //Returning a nullable Boolean where a primitive boolean is expected causes
        // auto-unboxing — which throws NullPointerException if the value is null.
        //Boolean.TRUE.equals(null) returns false safely — no NPE possible.
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String feedKey(Long userId) {
        return FEED_KEY_PREFIX + userId;
    }

    private String postCacheKey(Long postId) {
        return POST_CACHE_KEY_PREFIX + postId;
    }
}
