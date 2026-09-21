package com.thoughtsandopinions.apigateway.dto.api;

import jakarta.validation.constraints.NotBlank;

public record SignInDTO(
        @NotBlank(message = "Username cannot be blank")
        String username,
        
        @NotBlank(message = "Password cannot be blank")
        String password
) {
}
