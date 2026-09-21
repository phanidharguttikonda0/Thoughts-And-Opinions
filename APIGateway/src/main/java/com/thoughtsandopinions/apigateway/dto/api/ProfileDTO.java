package com.thoughtsandopinions.apigateway.dto.api;

import java.time.OffsetDateTime;

public record ProfileDTO(
        Long userId,
        String name,
        String username,
        String bio,
        String profilePic,
        int followersCount,
        int followingCount,
        OffsetDateTime joinedAt
) {}
