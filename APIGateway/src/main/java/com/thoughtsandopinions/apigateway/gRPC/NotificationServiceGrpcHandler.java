package com.thoughtsandopinions.apigateway.gRPC;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import notification.GetNotificationsRequest;
import notification.GetNotificationsResponse;
import notification.NotificationServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceGrpcHandler {
    @Value("${notification.grpc.host:localhost}")
    private String grpcHost;

    @Value("${notification.grpc.port:9095}")
    private int grpcPort;

    private ManagedChannel channel;
    private NotificationServiceGrpc.NotificationServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        blockingStub = NotificationServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public GetNotificationsResponse getNotifications(Long userId, int limit, String cursor) {
        GetNotificationsRequest.Builder request = GetNotificationsRequest.newBuilder()
                .setUserId(userId)
                .setLimit(limit);
        if (cursor != null) {
            request.setCursor(cursor);
        }
        return blockingStub.getNotifications(request.build());
    }
}
