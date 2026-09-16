package com.thoughtsandopinions.thoughtsservice.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class Thought {

    private final long thoughtId ;
    private final String content ;
    private final Long parentThoughtId ; // Long allows null values , long will not allow
    private final int likesCount ;
    private final int opinionsCount ;
    private final int repostsCount;
    private final OffsetDateTime createdAt ;

    public Thought(
            long thoughtId,
            String content,
            Long parentThoughtId,
            int likesCount,
            int opinionsCount,
            int repostsCount,
            OffsetDateTime createdAt
    ) {
        this.thoughtId = thoughtId;
        this.content = content;
        this.parentThoughtId = parentThoughtId;
        this.likesCount = likesCount;
        this.opinionsCount = opinionsCount;
        this.repostsCount = repostsCount;
        this.createdAt = createdAt;
    }

}
