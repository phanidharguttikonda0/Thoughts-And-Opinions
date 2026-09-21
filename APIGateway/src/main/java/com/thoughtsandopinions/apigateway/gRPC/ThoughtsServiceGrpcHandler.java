package com.thoughtsandopinions.apigateway.gRPC;

import com.google.protobuf.Empty;
import com.thoughtsandopinions.apigateway.dto.api.CreateThoughtDTO;
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

        Thoughts.GetUserHistoryRequest.Builder request = Thoughts.GetUserHistoryRequest.newBuilder()
                .setUserId(userId)
                .setLimit(limit) ;
        if (cursor != null) { request.setCursor(cursor); }
        return blockingStub.getUserHistory(request.build()) ;
    }

    public Thoughts.CreateResponse createThought(Long userId, CreateThoughtDTO thought) {

        Thoughts.CreateRequest.Builder request = Thoughts.CreateRequest.newBuilder() ;

        request.setUserId(userId) ;

        if(thought.parentThoughtId() != null){
            request.setParentThoughtId(thought.parentThoughtId()) ;
        }

        if(thought.content() != null) {
            request.setContent(thought.content()) ;
        }

        // media urls in next phase

        return blockingStub.createThought(request.build()) ;
    }


    public Empty deleteThought(Long userId, Long thoughtId) {

        Thoughts.DeleteRequest request = Thoughts.DeleteRequest.newBuilder()
                .setThoughtId(thoughtId)
                .setUserId(userId)
                .build() ;
        return blockingStub.deleteThought(request) ;
    }

    public Empty likeThought(Long userId, Long thoughtId) {

        Thoughts.LikeRequest request = Thoughts.LikeRequest.newBuilder()
                .setThoughtId(thoughtId)
                .setUserId(userId)
                .build() ;
        return blockingStub.likeThought(request) ;
    }

    public Empty unLikeThought(Long userId, Long thoughtId) {

        Thoughts.LikeRequest request = Thoughts.LikeRequest.newBuilder()
                .setThoughtId(thoughtId)
                .setUserId(userId)
                .build() ;
        return blockingStub.unlikeThought(request) ;
    }


    public Thoughts.GetLikesResponse getThoughtLikes(Long thoughtId, int limit, String cursor) {
        Thoughts.GetLikesRequest.Builder request =  Thoughts.GetLikesRequest.newBuilder()
                .setThoughtId(thoughtId)
                .setLimit(limit);
        if (cursor != null) { request.setCursor(cursor); }
        return blockingStub.getThoughtLikes(request.build()) ;
    }


    public Thoughts.GetRepostsResponse getThoughtReposts(Long thoughtId, int limit, String cursor) {
        Thoughts.GetRepostsRequest.Builder request = Thoughts.GetRepostsRequest.newBuilder()
                .setThoughtId(thoughtId)
                .setLimit(limit) ;
        if (cursor != null) { request.setCursor(cursor); }
        return blockingStub.getReposts(request.build()) ;
    }

    public Thoughts.GetThoughtResponse getThought(Long thoughtId) {
        Thoughts.GetThoughtRequest request = Thoughts.GetThoughtRequest.newBuilder().setThoughtId(thoughtId).build() ;
        return blockingStub.getThought(request) ;
    }


    public Thoughts.GetOpinionsResponse getOpinions(Long thoughtId, int limit, String cursor) {

        Thoughts.GetOpinionsRequest.Builder request = Thoughts.GetOpinionsRequest.newBuilder()
                .setThoughtId(thoughtId)
                .setLimit(limit);
        if (cursor != null) { request.setCursor(cursor); }
        return blockingStub.getOpinions(request.build()) ;
    }

}
