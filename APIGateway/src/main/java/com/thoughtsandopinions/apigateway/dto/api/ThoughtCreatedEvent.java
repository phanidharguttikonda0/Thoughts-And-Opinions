package com.thoughtsandopinions.apigateway.dto.api;

public record ThoughtCreatedEvent(
        String thoughtId,
        String authorId,
        long createdAt
) {}
