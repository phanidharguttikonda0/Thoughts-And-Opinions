package com.thoughtsandopinions.apigateway.controller;

import com.thoughtsandopinions.apigateway.dto.api.NotificationDTO;
import com.thoughtsandopinions.apigateway.dto.api.NotificationsFeedDTO;
import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.gRPC.NotificationServiceGrpcHandler;
import notification.GetNotificationsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationServiceGrpcHandler notificationServiceGrpcHandler;

    public NotificationController(NotificationServiceGrpcHandler notificationServiceGrpcHandler) {
        this.notificationServiceGrpcHandler = notificationServiceGrpcHandler;
    }

    @GetMapping
    public Mono<ResponseEntity<ResponseDTO<NotificationsFeedDTO>>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {

        return Mono.fromCallable(() -> notificationServiceGrpcHandler.getNotifications(userId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    List<NotificationDTO> notifications = response.getNotificationsList().stream()
                            .map(item -> new NotificationDTO(
                                    item.getNotificationId(),
                                    item.getType(),
                                    item.getActorId(),
                                    item.getActorUsername(),
                                    item.getThoughtId(),
                                    item.getTimestamp()
                            ))
                            .toList();

                    String nextCursor = response.getNextCursor().isEmpty() ? null : response.getNextCursor();
                    NotificationsFeedDTO feedDTO = new NotificationsFeedDTO(notifications, nextCursor);

                    return ResponseEntity.ok(
                            ResponseDTO.<NotificationsFeedDTO>builder()
                                    .success(true)
                                    .message("Notifications retrieved successfully")
                                    .data(feedDTO)
                                    .build()
                    );
                });
    }
}
