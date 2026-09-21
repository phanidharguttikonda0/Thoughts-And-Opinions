package com.thoughtsandopinions.apigateway.dto.api;

public record UserDTO(
        Long userId,
        String name,
        String username,
        String profilePicUrl
) {}
