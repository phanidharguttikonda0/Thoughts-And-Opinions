package com.thoughtsandopinions.timelineservice.dto;

/**
 * Event consumed from ThoughtService to fan-out the thought to followers' feeds.
 */
public record ThoughtCreatedEvent(
        String thoughtId,
        String authorId,
        long createdAt // Unix timestamp in milliseconds will be used as the ZSET score,
        // for chronological ordering
) {}