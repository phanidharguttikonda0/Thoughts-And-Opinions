package com.thoughtsandopinions.apigateway.dto.service;

public record UserCache(
        Long userId,
        String username,
        String name,
        String profilePicUrl
) {
}
