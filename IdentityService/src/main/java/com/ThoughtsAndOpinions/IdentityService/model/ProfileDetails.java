package com.ThoughtsAndOpinions.IdentityService.model;

import java.time.OffsetDateTime;

public record ProfileDetails(
        long userId,
        String username,
        String name,
        String profilePicUrl,
        OffsetDateTime createdAt
) {
}


