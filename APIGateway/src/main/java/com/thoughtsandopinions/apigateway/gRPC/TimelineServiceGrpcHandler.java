package com.thoughtsandopinions.apigateway.gRPC;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import timeline.Timeline;
import timeline.TimelineGatewayServiceGrpc;

@Service
public class TimelineServiceGrpcHandler {
    @Value("${timeline.grpc.host:localhost}")
    private String grpcHost;

    @Value("${timeline.grpc.port:9094}")
    private int grpcPort;

    private ManagedChannel channel;
    private TimelineGatewayServiceGrpc.TimelineGatewayServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        blockingStub = TimelineGatewayServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public Timeline.GetFeedResponse getFeed(Long userId, int limit, String cursor) {
        Timeline.GetFeedRequest.Builder request = Timeline.GetFeedRequest.newBuilder()
                .setUserId(userId)
                .setLimit(limit);
        if (cursor != null) {
            request.setCursor(cursor);
        }
        return blockingStub.getFeed(request.build());
    }
}
