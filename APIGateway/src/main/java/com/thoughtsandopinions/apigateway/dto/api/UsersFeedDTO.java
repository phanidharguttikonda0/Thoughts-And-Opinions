package com.thoughtsandopinions.apigateway.dto.api;

import java.util.List;

public record UsersFeedDTO(
        List<UserDTO> users,
        String nextCursor
) {}
