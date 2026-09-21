package com.thoughtsandopinions.timelineservice.grpc;

import identity.IdentityGatewayServiceGrpc;
import identity.IdentityGatewayServiceGrpc.IdentityGatewayServiceBlockingStub;
import identity.UsersListRequest;
import identity.UsersListResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdentityServiceGrpcHandler {

    @Value("${identity.grpc.host:localhost}")
    private String grpcHost;

    @Value("${identity.grpc.port:9090}")
    private int grpcPort;

    private ManagedChannel channel;
    private IdentityGatewayServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        blockingStub = IdentityGatewayServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public UsersListResponse getFollowersList(Long userId, int limit, String cursor) {
        UsersListRequest.Builder request = UsersListRequest.newBuilder()
                .setUserId(userId)
                .setLimit(limit);
        
        if (cursor != null && !cursor.isEmpty()) {
            request.setCursor(cursor);
        }
        
        return blockingStub.getFollowersList(request.build());
    }
}
