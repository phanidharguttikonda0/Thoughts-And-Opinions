package com.thoughtsandopinions.apigateway.gRPC;

import identity.AuthResponse;
import identity.IdentityGatewayServiceGrpc;
import identity.IdentityGatewayServiceGrpc.IdentityGatewayServiceBlockingStub;
import identity.SignUpRequest;
import identity.LoginRequest ;
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

    public AuthResponse signUp(String username, String email, String password) {
        SignUpRequest request = SignUpRequest.newBuilder()
                .setUsername(username)
                .setEmail(email)
                .setPassword(password)
                .setName(username)
                .build();
        return blockingStub.signUp(request);
    }

    public AuthResponse signIn(String username, String password) {
        LoginRequest request = LoginRequest.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build() ;
        return blockingStub.login(request) ;
    }

}
