package com.ThoughtsAndOpinions.IdentityService.model;

import java.time.OffsetDateTime;

public record Profile(
        long userId,
        String username,
        String name,
        String bio,
        String profilePicUrl,
        int followersCount,
        int followingCount,
        OffsetDateTime createdAt
) {

}
