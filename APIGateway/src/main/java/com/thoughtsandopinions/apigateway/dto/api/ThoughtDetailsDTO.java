package com.thoughtsandopinions.apigateway.dto.api;

import java.time.OffsetDateTime;

public record ThoughtDetailsDTO (
        Long thoughtId,
        // User will be populated when getting the live feed.
        // For the profile feed, this will be null as the user context is already known.
        UserDTO user,
        String content,
        int likesCount,
        int opinionsCount,
        int repostsCount,
        Long parentThoughtId,
        OffsetDateTime createdAt,
        boolean isLiked,
        boolean isReposted
) {}
