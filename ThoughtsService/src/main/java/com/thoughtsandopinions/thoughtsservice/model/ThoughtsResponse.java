package com.thoughtsandopinions.thoughtsservice.model;

import java.time.OffsetDateTime;

public record ThoughtsResponse(
        long thoughtId,
        OffsetDateTime createdAt
) {
}
