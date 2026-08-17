package com.project.SocialMediaFeedService.feed.service.impl;

import com.project.SocialMediaFeedService.feed.dto.response.FeedResponse;
import com.project.SocialMediaFeedService.feed.repository.FeedRedisRepository;
import com.project.SocialMediaFeedService.feed.service.FeedService;
import com.project.SocialMediaFeedService.follow.repository.FollowRepository;
import com.project.SocialMediaFeedService.post.dto.response.PostResponse;
import com.project.SocialMediaFeedService.post.entity.Post;
import com.project.SocialMediaFeedService.post.mapper.PostMapper;
import com.project.SocialMediaFeedService.post.repository.PostRepository;
import com.project.SocialMediaFeedService.user.entity.User;
import com.project.SocialMediaFeedService.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {
    private final FeedRedisRepository feedRedisRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final ObjectMapper objectMapper;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    private static final long CELEBRITY_THRESHOLD = 10_000L;
    private static final int FEED_REBUILD_LIMIT = 50;
    private static final String CELEBRITY = "celebrity";
    private static final String REGULAR = "regular";

    @Override
    @Transactional(readOnly = true)
    public FeedResponse getFeed(Long userId, String cursor, int limit) {
        log.info("getFeed called for userId={} cursor={} limit={}", userId, cursor, limit);
        // Step 1: Parse cursor
        double score = parseCursorToScore(cursor);
        LocalDateTime cursorTime = parseCursorToDateTime(cursor);

        // Step 2: Get following IDs
        List<Long> followingIds = followRepository.findFollowingIdsByUserId(userId);
        if (followingIds.isEmpty()) {
            return emptyFeed();
        }

        // Step 3: Split into regular and celebrity
        Map<String, List<Long>> split = splitFollowingByType(followingIds);
        List<Long> regularIds = split.get(REGULAR);
        List<Long> celebrityIds = split.get(CELEBRITY);
        log.info("Following count={} regular={} celebrity={}",
                followingIds.size(), regularIds.size(), celebrityIds.size());

        // Step 4: Get regular posts
        List<PostResponse> regularPosts = getRegularPosts(userId, regularIds, cursor, score, limit, cursorTime);

        // Step 5: Get celebrity posts
        List<PostResponse> celebrityPosts = getCelebrityPosts(celebrityIds, cursor,cursorTime, limit);

        // Step 6: Merge
        List<PostResponse> merged = new ArrayList<>(regularPosts);
        merged.addAll(celebrityPosts);
        merged.sort(Comparator.comparing(PostResponse::getCreatedAt).reversed());


        // Step 7: Determine hasMore and trim
        boolean hasMore = merged.size() > limit;
        if(hasMore){
            merged = new ArrayList<>(merged.subList(0, limit));  // safer — makes it mutable
        }

        // Step 8: Calculate nextCursor
        String nextCursor = hasMore && !merged.isEmpty() ? String.valueOf(merged.getLast()
                .getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
                : null;

        log.info("Returning feed size={} hasMore={} nextCursor={}",
                merged.size(), hasMore, nextCursor);

        return FeedResponse.builder()
                .posts(merged)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private double parseCursorToScore(String cursor){
        //If cursor is null or empty → use Double.MAX_VALUE
        //Else → parse cursor to double
        //Use cursor - 1 to exclude the cursor item itself
        return cursor == null || cursor.isBlank() ? Double.MAX_VALUE : Double.parseDouble(cursor);
    }

    private LocalDateTime parseCursorToDateTime(String cursor){
        // Returns LocalDateTime.now(ZoneOffset.UTC) if cursor is null
        // Returns Instant.ofEpochMilli(Long.parseLong(cursor))
        //              .atZone(ZoneId.of("UTC"))
        //              .toLocalDateTime() otherwise
        if(cursor == null){
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return Instant.ofEpochMilli(Long.parseLong(cursor)).atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    private Map<String, List<Long>> splitFollowingByType(List<Long> followingIds){
        Map<String, List<Long>> regularAndCelebrityMap = new HashMap<>();
        //If Alice follows nobody with celebrity status it will return null — key not in map so initialized it before.
        regularAndCelebrityMap.putIfAbsent(REGULAR, new ArrayList<>());
        regularAndCelebrityMap.putIfAbsent(CELEBRITY, new ArrayList<>());
        // Fetch all followed users in one query: userRepository.findAllById(followingIds)
        // For each user:
        //   followerCount >= CELEBRITY_THRESHOLD → add to celebrityIds
        //   else → add to regularIds
        // Return map with keys "regular" and "celebrity"
        List<User> userIds = userRepository.findAllById(followingIds);

        for(User user: userIds){
            if(user.getFollowerCount() >= CELEBRITY_THRESHOLD){
                regularAndCelebrityMap.get(CELEBRITY).add(user.getId());
            }else{
                regularAndCelebrityMap.get(REGULAR).add(user.getId());
            }
        }
        return regularAndCelebrityMap;
    }

    private List<PostResponse> getRegularPosts(
            Long userId, List<Long> regularIds, String cursor, double score, int limit, LocalDateTime before){
        ArrayList<PostResponse> cacheHitPosts = new ArrayList<>();
        ArrayList<Long> cacheMissPostId = new ArrayList<>();
        List<PostResponse> allPosts = new ArrayList<>();
        // If regularIds empty → return empty list
        if(regularIds == null || regularIds.isEmpty()){
            return List.of();
        }
        // Check feedExists(userId)
        if(feedRedisRepository.feedExists(userId)){
            log.info("redis feed exists for regular ids,calling for if condition for regularPost");
            // If exists:
            //   getFeedPostIds(userId, score, limit+1) → Set<String>
            Set<String> feedPostIds = feedRedisRepository.getFeedPostIds(userId, score, limit + 1);
            // Reset TTL since user is actively reading their feed
            feedRedisRepository.resetFeedTtl(userId);
            //   Convert to List<Long>
            List<Long> postIds = feedPostIds.stream().map(Long::parseLong).toList();
            //   Batch fetch with cache-aside (existing logic)
            //Separate postIds into cached and uncached
            //        → For each postId:
            //           → Try getCachedPost(postId)
            //           → If present → deserialize JSON → add to results
            //           → If empty → add to missedIds list
            getCachedPostsFromRedisCache(postIds, cacheHitPosts, cacheMissPostId);
            //Batch fetch posts from PostgresSQL which are missed from redis cache
            List<Post> postsFromDb = postRepository.findAllById(cacheMissPostId);
            //cache newly fetched posts
            cachePostsFetchedFromDb(postsFromDb, allPosts);
            //Combine cached posts + newly fetched posts
            allPosts.addAll(cacheHitPosts);
        }else{
            log.info("redis feed empty for regular ids,calling for else condition for regularPost");
            // If not exists (Redis empty):
            //   findRecentPostsByUserIds(regularIds, FEED_REBUILD_LIMIT)
            List<Post> recentPostsByUserIds;
            if(cursor == null || cursor.isBlank()){
                recentPostsByUserIds = postRepository.findRecentPostsByUserIds(regularIds, FEED_REBUILD_LIMIT);

            }else{
                recentPostsByUserIds = postRepository.findRecentPostsByUserIdsAndBefore(regularIds,before,FEED_REBUILD_LIMIT);
            }
            //   Write postIds back to Redis:
            //     for each post: addToFeed(userId, post.getId(), score from createdAt)
            //   Convert to PostResponse
            for(Post recentPost : recentPostsByUserIds){
                feedRedisRepository.addToFeed(userId,recentPost.getId(),calculateScore(recentPost.getCreatedAt()));
                allPosts.add(postMapper.toResponse(recentPost));
            }
        }
        return allPosts;
    }

    private List<PostResponse> getCelebrityPosts(
            List<Long> celebrityIds, String cursor, LocalDateTime before, int limit){
        List<PostResponse> allPosts = new ArrayList<>();
        // If celebrityIds empty → return empty list
        if(celebrityIds == null || celebrityIds.isEmpty()){
            return List.of();
        }
        //if cursor is null/blank call findRecentPostsByUserIds else call findRecentPostsByUserIdsAndBefore
        //limit+1 to check if we have more posts-we remove it later.
        List<Post> posts;
        if(cursor == null || cursor.isBlank()){
            posts = postRepository.findRecentPostsByUserIds(celebrityIds,limit+1);
        }else{
            posts = postRepository.findRecentPostsByUserIdsAndBefore(celebrityIds, before, limit+1);
        }
        // Convert each Post to PostResponse using postMapper
        for(Post p: posts){
            allPosts.add(postMapper.toResponse(p));
        }
        // Return list
        return allPosts;
    }

    private double calculateScore(LocalDateTime createdTimestamp){
        return createdTimestamp
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    private FeedResponse emptyFeed() {
        return FeedResponse.builder()
                .posts(List.of())
                .nextCursor(null)
                .hasMore(false)
                .build();
    }

    private void getCachedPostsFromRedisCache(List<Long> postIds, ArrayList<PostResponse> cacheHitPosts, ArrayList<Long> cacheMissPostId){
        for(Long postId : postIds){
            Optional<String> cachedPost = feedRedisRepository.getCachedPost(postId);
            if(cachedPost.isPresent()){
                log.info("Cache hit for postId : {}", postId);
                try{
                    String json = cachedPost.get();
                    PostResponse  post = objectMapper.readValue(json, PostResponse.class);
                    cacheHitPosts.add(post);
                } catch (JacksonException e) {
                    log.warn("Exception hit while serialization during cache read for postId: {}", postId);
                    // If JSON parsing fails, treat it as a cache miss to be safe
                    cacheMissPostId.add(postId);
                }
            }else{
                cacheMissPostId.add(postId);
            }
        }
    }

    private void cachePostsFetchedFromDb(List<Post> posts, List<PostResponse> allPosts){
        for(Post post: posts){
            PostResponse postToBeCached = postMapper.toResponse(post);
            try{
                log.info("Caching the post fetched from DB. post: {}", post);
                String jsonToBeCached = objectMapper.writeValueAsString(postToBeCached);
                feedRedisRepository.cachePost(post.getId(),jsonToBeCached);
            } catch (JacksonException e) {
                log.warn("Exception hit while serialization during cache write for post: {}", post);
                log.warn("Error in Caching the Post");
            }
            allPosts.add(postToBeCached);
        }
    }

}
