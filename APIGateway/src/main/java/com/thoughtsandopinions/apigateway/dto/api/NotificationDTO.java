package com.thoughtsandopinions.apigateway.dto.api;

public record NotificationDTO(
        String notificationId,
        String type,           // LIKE, REPOST, OPINION, FOLLOW
        long actorId,
        String actorUsername,
        long thoughtId,        // 0 for FOLLOW
        long timestamp         // Unix millis
) {}
