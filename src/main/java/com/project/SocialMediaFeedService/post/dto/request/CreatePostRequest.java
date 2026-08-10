package com.project.SocialMediaFeedService.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostRequest {

    @NotBlank
    @Size(min = 1, max = 3000,message = "content cannot be less than 1 character or exceeds 3000 characters.")
    private String content;
}
