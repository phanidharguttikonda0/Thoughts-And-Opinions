package com.ThoughtsAndOpinions.IdentityService.exception;

public class InCorrectCredentials extends RuntimeException {
    public InCorrectCredentials(String message) {
        super(message);
    }
}
