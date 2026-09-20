package com.thoughtsandopinions.apigateway.gRPC;

import com.google.protobuf.Empty;
import com.thoughtsandopinions.apigateway.dto.service.UserCache;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import thoughts.ThoughtGatewayServiceGrpc;
import thoughts.Thoughts;

@Service
public class ThoughtsServiceGrpcHandler {
    @Value("${thoughts.grpc.host:localhost}")
    private String grpcHost;

    @Value("${thoughts.grpc.port:9090}")
    private int grpcPort;

    private ManagedChannel channel;
    private ThoughtGatewayServiceGrpc.ThoughtGatewayServiceBlockingStub blockingStub;

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


    public Empty userCache(UserCache user) {
        Thoughts.Users.Builder userBuilder = Thoughts.Users.newBuilder() ;

        userBuilder.setUserId(user.userId()) ;

        if(user.username() != null) {
            userBuilder.setUsername(user.username()) ;
        }

        if(user.name() != null ){
            userBuilder.setName(user.name()) ;
        }

        if(user.profilePicUrl() != null) {
            userBuilder.setProfilePicUrl(user.profilePicUrl()) ;
        }

        return blockingStub.storeUser(userBuilder.build()) ;
    }

    public Thoughts.GetHistoryResponse getUserProfileFeed(Long userId, int limit, String cursor) {

        Thoughts.GetUserHistoryRequest request = Thoughts.GetUserHistoryRequest.newBuilder()
                .setCursor(cursor)
                .setUserId(userId)
                .setLimit(limit).build() ;

        return blockingStub.getUserHistory(request) ;
    }

}
