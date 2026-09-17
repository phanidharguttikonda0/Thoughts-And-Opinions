package com.thoughtsandopinions.thoughtsservice.exception;

public class DuplicateRepostException extends RuntimeException {
    public DuplicateRepostException(String message) {
        super(message);
    }
}
