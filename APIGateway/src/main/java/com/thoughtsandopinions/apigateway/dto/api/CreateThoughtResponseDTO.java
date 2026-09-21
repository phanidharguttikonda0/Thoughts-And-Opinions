package com.thoughtsandopinions.apigateway.dto.api;

import java.time.OffsetDateTime;

public record CreateThoughtResponseDTO(
        Long thoughtId,
        OffsetDateTime createdAt
) {}
