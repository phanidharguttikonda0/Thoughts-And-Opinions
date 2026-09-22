package com.thoughtsandopinions.timelineservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import thoughts.Thoughts.GetThoughtRequest;
import thoughts.Thoughts.GetThoughtResponse;
import thoughts.ThoughtGatewayServiceGrpc;
import thoughts.ThoughtGatewayServiceGrpc.ThoughtGatewayServiceBlockingStub;

@Service
public class ThoughtsServiceGrpcHandler {

    @Value("${thoughts.grpc.host:localhost}")
    private String grpcHost;

    @Value("${thoughts.grpc.port:9091}")
    private int grpcPort;

    private ManagedChannel channel;
    private ThoughtGatewayServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        blockingStub = ThoughtGatewayServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public GetThoughtResponse getThought(Long thoughtId) {
        GetThoughtRequest request = GetThoughtRequest.newBuilder()
                .setThoughtId(thoughtId)
                .build();
        
        return blockingStub.getThought(request);
    }
}
