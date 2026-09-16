package com.thoughtsandopinions.thoughtsservice.exception;

public class ThoughtNotFoundException extends RuntimeException {
    public ThoughtNotFoundException(String message) {
        super(message);
    }
}
