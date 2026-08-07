package com.project.SocialMediaFeedService.follow.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

//In JPA, composite primary keys require a separate class to hold the key fields.
//
//This class:
//
//Must implement Serializable
//Must have @Embeddable annotation
//Must have both fields that form the key
//Must have equals and hashCode implemented — Lombok can handle this
//Then in the Follow entity you use @EmbeddedId instead of @Id.

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class FollowId implements Serializable {
    private Long followerId;
    private Long followingId;
}
