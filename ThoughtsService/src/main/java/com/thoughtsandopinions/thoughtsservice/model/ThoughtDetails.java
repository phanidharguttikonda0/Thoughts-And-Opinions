package com.thoughtsandopinions.thoughtsservice.model;


import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ThoughtDetails {

    private final long thoughtId ;
    private final String username ;
    private final long userId ;
    private final String name ;
    private final String profilePicUrl ;
    private final String content ;
    private final long parentThoughtId ;
    private final int likesCount ;
    private final int opinionsCount ;
    private final int repostsCount;
    private final OffsetDateTime createdAt ;

    public ThoughtDetails(
            long thoughtId,
            String username,
            long userId,
            String name,
            String profilePicUrl,
            String content,
            long parentThoughtId,
            int likesCount,
            int opinionsCount,
            int repostsCount,
            OffsetDateTime createdAt
    ) {
        this.thoughtId = thoughtId;
        this.username = username;
        this.userId = userId;
        this.name = name;
        this.profilePicUrl = profilePicUrl;
        this.content = content;
        this.parentThoughtId = parentThoughtId;
        this.likesCount = likesCount;
        this.opinionsCount = opinionsCount;
        this.repostsCount = repostsCount;
        this.createdAt = createdAt;
    }
}


/*
* message GetThoughtResponse {
  int64 thought_id = 1;
  Users user = 2 ;
  string content = 3;
  int32 likes_count = 4;
  int32 opinions_count = 5; // when we click on opinions, we will return list of opinions via cursor pagination
  int32 reposts_count = 6;
  int64 parent_thought_id = 7;
  google.protobuf.Timestamp createdAt = 8;
}
*
* */