package com.project.SocialMediaFeedService.feed.service.impl;

import com.project.SocialMediaFeedService.feed.dto.response.FeedResponse;
import com.project.SocialMediaFeedService.feed.repository.FeedRedisRepository;
import com.project.SocialMediaFeedService.feed.service.FeedService;
import com.project.SocialMediaFeedService.post.dto.response.PostResponse;
import com.project.SocialMediaFeedService.post.entity.Post;
import com.project.SocialMediaFeedService.post.mapper.PostMapper;
import com.project.SocialMediaFeedService.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;



import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {
    private final FeedRedisRepository feedRedisRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional(readOnly = true)
    public FeedResponse getFeed(Long userId, String cursor, int limit) {
        boolean hasMoreFeed = false;
        ArrayList<PostResponse> cacheHitPosts = new ArrayList<>();
        ArrayList<Long> cacheMissPostId = new ArrayList<>();
        List<PostResponse> freshPosts = new ArrayList<>();

        //If cursor is null or empty → use Double.MAX_VALUE
        //Else → parse cursor to double
        //Use cursor - 1 to exclude the cursor item itself

        double formattedCursor = cursor == null || cursor.isBlank() ? Double.MAX_VALUE : Double.parseDouble(cursor) -1;

        //Get postIds from Redis
        //Request limit + 1 postIds from Redis feed
        //If we get limit + 1 → hasMore = true, remove last one
        //If less → hasMore = false
        Set<String> feedPostIds = feedRedisRepository.getFeedPostIds(userId, formattedCursor, limit + 1);

        //If no postIds → return empty feed
        if(!feedPostIds.isEmpty()){
            log.info("feedPostIds returned from Redis are {}", feedPostIds);
            if(feedPostIds.size() > limit){
                hasMoreFeed = true;
                log.info("we have more feed to load");
            }
        }else{
            log.info("-----------Returning Empty Feed---------");
                return FeedResponse.builder()
                        .posts(List.of())
                        .nextCursor(null)
                        .hasMore(false)
                        .build();
        }

        //Convert postIds to List<Long>
        List<Long> postIds = feedPostIds.stream().map(Long::parseLong).collect(Collectors.toList());

        if (hasMoreFeed) {
            postIds.removeLast();  // only remove the extra one when there are more
        }

        //Separate postIds into cached and uncached
        //        → For each postId:
        //           → Try getCachedPost(postId)
        //           → If present → deserialize JSON → add to results
        //           → If empty → add to missedIds list
        getCachedPostsFromRedisCache(postIds, cacheHitPosts, cacheMissPostId);


        //Batch fetch posts from PostgresSQL which are missed from redis cache
        List<Post> postsFromDb = postRepository.findAllById(cacheMissPostId);

        //cache newly fetched posts
        cachePostsFetchedFromDb(postsFromDb, freshPosts);

        //Combine cached posts + newly fetched posts
        List<PostResponse> allPosts = new ArrayList<>(cacheHitPosts);
        allPosts.addAll(freshPosts);


        //Convert to PostResponse list
        // Sort by createdAt descending
        List<PostResponse> postFeed = allPosts.stream()
                .sorted(Comparator.comparing(PostResponse::getCreatedAt).reversed()).collect(Collectors.toList());
        log.info("----------PostResponse : {}", postFeed);


        //Calculate nextCursor
        String nextCursor = hasMoreFeed && !postFeed.isEmpty()
                ? String.valueOf(postFeed.getLast().getCreatedAt()
                .toInstant(ZoneOffset.UTC).toEpochMilli())
                : null;

        //build and return
        log.info("---------------Returning PostFeed: {}, nextCursor: {}, hasMoreFeed: {}", postFeed,nextCursor,hasMoreFeed);
        return new FeedResponse(postFeed,nextCursor,hasMoreFeed);
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

    private void cachePostsFetchedFromDb(List<Post> posts, List<PostResponse> freshPosts){
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
            freshPosts.add(postToBeCached);
        }
    }

}
