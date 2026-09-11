package com.ThoughtsAndOpinions.IdentityService.exception;

public class UserExistsException extends RuntimeException{
    private final UserExistsType type ;

    public UserExistsException(UserExistsType type) {
        this.type = type ;
    }

    public UserExistsType getType(){
        return this.type ;
    }
}
