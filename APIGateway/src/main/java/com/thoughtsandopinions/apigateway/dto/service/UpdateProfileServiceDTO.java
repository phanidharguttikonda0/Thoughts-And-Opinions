package com.thoughtsandopinions.apigateway.dto.service;

public record UpdateProfileServiceDTO (
        String name,
        String username,
        Long userId,
        String bio,
        String profilePicUrl
){
}
