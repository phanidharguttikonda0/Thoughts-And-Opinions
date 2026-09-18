package com.thoughtsandopinions.apigateway.dto;

public record SignUpDTO(
        String username,
        String email,
        String password
) {
}
