package com.thoughtsandopinions.apigateway.dto.api;

import java.util.List;

public record NotificationsFeedDTO(
        List<NotificationDTO> notifications,
        String nextCursor
) {}
