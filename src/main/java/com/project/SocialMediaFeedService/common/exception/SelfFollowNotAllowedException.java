package com.project.SocialMediaFeedService.common.exception;

public class SelfFollowNotAllowedException extends RuntimeException{
    public SelfFollowNotAllowedException(String message){
        super(message);
    }
}
