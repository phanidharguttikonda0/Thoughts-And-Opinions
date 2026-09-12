package com.ThoughtsAndOpinions.IdentityService.model;

public record ProfileDetails(
        long userId,
        String username,
        String name,
        String profilePicUrl
) {
}


