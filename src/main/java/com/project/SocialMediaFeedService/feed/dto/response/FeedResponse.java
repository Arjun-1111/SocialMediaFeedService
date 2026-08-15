package com.project.SocialMediaFeedService.feed.dto.response;

import com.project.SocialMediaFeedService.post.dto.response.PostResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FeedResponse {

    private List<PostResponse> posts ;

    private String nextCursor;

    private boolean hasMore;

}
