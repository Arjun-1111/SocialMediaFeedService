package com.project.SocialMediaFeedService.follow.service.impl;

import com.project.SocialMediaFeedService.common.exception.DuplicateResourceException;
import com.project.SocialMediaFeedService.common.exception.ResourceNotFoundException;
import com.project.SocialMediaFeedService.common.exception.SelfFollowNotAllowedException;
import com.project.SocialMediaFeedService.follow.dto.response.FollowResponse;
import com.project.SocialMediaFeedService.follow.entity.Follow;
import com.project.SocialMediaFeedService.follow.entity.FollowId;
import com.project.SocialMediaFeedService.follow.repository.FollowRepository;
import com.project.SocialMediaFeedService.follow.service.FollowService;
import com.project.SocialMediaFeedService.user.entity.User;
import com.project.SocialMediaFeedService.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FollowResponse follow(Long followerId, Long followingId) {
        //check if followerId matches the followingId
        checkIfFollowerIdAndFollowingIdMatches(followerId,followingId);

        //check both user exits
        checkIfBothUserExists(followerId,followingId);

        //create FollowId
        FollowId followId = new FollowId(followerId,followingId);

        //check if already following
        checkIfUserIsAlreadyFollowing(followId);

        //build and save follow entity
        Follow follow = new Follow();
        follow.setId(followId);
        follow.setFollower(userRepository.getReferenceById(followerId)); //creating a User reference . getReferenceById - Returns a proxy — does NOT hit database unless fields are accessed
        follow.setFollowing(userRepository.getReferenceById(followingId));

        userRepository.incrementFollowerCount(followingId);
        userRepository.incrementFollowingCount(followerId);

        Follow savedFollow = followRepository.save(follow);

        //fetch updated following Count
        User userBeingFollowed  = userRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User userDoingFollowing  = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));


        //Build and return FollowResponse
        return FollowResponse.builder()
                .followerCount(userBeingFollowed.getFollowerCount())   // User B gained a follower
                .followingCount(userDoingFollowing.getFollowingCount())  // User A is following one more
                .followedAt(savedFollow.getCreatedAt())
                .followerId(followerId)
                .followingId(followingId)
                .build();
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        //check if followerId matches the followingId
        checkIfFollowerIdAndFollowingIdMatches(followerId,followingId);

        //check both user exits
        checkIfBothUserExists(followerId,followingId);

        //create FollowId
        FollowId followId = new FollowId(followerId,followingId);

        //check if already following
        if (!followRepository.existsById(followId)) {
            throw new ResourceNotFoundException("Follow relationship not found");
        }


        //delete the user and decrement count
        followRepository.deleteById(followId);

        userRepository.decrementFollowerCount(followingId);
        userRepository.decrementFollowingCount(followerId);


    }

    private void checkIfFollowerIdAndFollowingIdMatches(Long followerId, Long followingId){
        if(followerId.equals(followingId)){
            throw new SelfFollowNotAllowedException("user cannot follow themselves");
        }
    }

    private void checkIfBothUserExists(Long followerId, Long followingId){
        if(!userRepository.existsById(followerId) || !userRepository.existsById(followingId)){
            throw new ResourceNotFoundException("user not found");
        }
    }

    private void checkIfUserIsAlreadyFollowing(FollowId followId){
        if(followRepository.existsById(followId)){
            throw new DuplicateResourceException("user already followed the followingId");
        }
    }
}
