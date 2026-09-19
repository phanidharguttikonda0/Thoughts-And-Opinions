package com.thoughtsandopinions.apigateway.dto.api;

public record SignUpDTO(
        String username,
        String email,
        String password
) {
}
