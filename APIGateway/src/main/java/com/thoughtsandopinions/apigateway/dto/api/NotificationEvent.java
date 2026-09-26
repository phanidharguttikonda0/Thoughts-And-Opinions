package com.thoughtsandopinions.apigateway.dto.api;

/**
 * Kafka event published to the 'notification.events' topic.
 * Consumed by the Go-based NotificationService.
 */
public record NotificationEvent(
        String type,           // LIKE, REPOST, OPINION, FOLLOW
        long actorId,          // User who performed the action
        String actorUsername,  // Username of the actor (for display)
        long targetUserId,    // User who receives the notification
        long thoughtId,       // Relevant thought ID (0 for FOLLOW)
        long timestamp        // Unix millis
) {}
