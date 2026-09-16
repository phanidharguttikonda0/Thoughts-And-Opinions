package com.thoughtsandopinions.thoughtsservice.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserActivitySummary {
    private final long userId;
    private final String username;
    private final String name;
    private final String profilePicUrl;
    private final OffsetDateTime activityCreatedAt; // when they liked/reposted — used to build the next cursor

    public UserActivitySummary(long userId, String username, String name,
                               String profilePicUrl, OffsetDateTime activityCreatedAt) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.profilePicUrl = profilePicUrl;
        this.activityCreatedAt = activityCreatedAt;
    }
}