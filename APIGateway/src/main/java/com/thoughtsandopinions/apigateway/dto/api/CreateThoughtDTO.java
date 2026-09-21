package com.thoughtsandopinions.apigateway.dto.api;

public record CreateThoughtDTO(
        String content,
        Long parentThoughtId
) {
}
