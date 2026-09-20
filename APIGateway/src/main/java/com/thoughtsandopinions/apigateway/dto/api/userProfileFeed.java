package com.thoughtsandopinions.apigateway.dto.api;

import thoughts.Thoughts;

import java.util.List;

public record userProfileFeed(
        List<Thoughts.ThoughtDetails> thoughts,
        String nextCursor
) {
}
