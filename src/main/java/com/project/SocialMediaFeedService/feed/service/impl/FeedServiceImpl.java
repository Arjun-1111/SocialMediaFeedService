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

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {
    private final FeedRedisRepository feedRedisRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    @Override
    @Transactional(readOnly = true)
    public FeedResponse getFeed(Long userId, String cursor, int limit) {
        boolean hasMoreFeed = false;

        //If cursor is null or empty → use Double.MAX_VALUE
        //Else → parse cursor to double
        double formattedCursor = cursor == null || cursor.isBlank() ? Double.MAX_VALUE : Double.parseDouble(cursor);

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

        //Batch fetch posts from PostgreSQL
        List<Post> postsById = postRepository.findAllById(postIds);

        //Convert to PostResponse list
        // Sort by createdAt descending
        List<PostResponse> postFeed = postsById.stream()
                .map(postMapper::toResponse).sorted(Comparator.comparing(PostResponse::getCreatedAt).reversed()).collect(Collectors.toList());
        log.info("----------PostResponse : {}", postFeed);


        //Calculate nextCursor
        String nextCursor = hasMoreFeed && !postFeed.isEmpty()
                ? String.valueOf(postFeed.getLast().getCreatedAt()
                .toInstant(ZoneOffset.UTC).toEpochMilli())
                : null;

        //build and return
        log.info("PostFeed: {}, nextCursor: {}, hasMoreFeed: {}", postFeed,nextCursor,hasMoreFeed);
        return new FeedResponse(postFeed,nextCursor,hasMoreFeed);
    }
}
