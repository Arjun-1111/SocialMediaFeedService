package com.project.SocialMediaFeedService.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username should be between 3 and 50 characters")
    private String username;

    @Size(max = 500, message = "Bio should not exceed 500 characters")
    private String bio;

    @Size(max = 500, message = "Avatar URL should not exceed 500 characters")
    private String avatarUrl;

}
