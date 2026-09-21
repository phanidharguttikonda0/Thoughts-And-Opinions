package com.thoughtsandopinions.apigateway.dto.api;

import java.util.List;

public record ThoughtsFeedDTO(
        List<ThoughtDetailsDTO> thoughts,
        String nextCursor
) {}
