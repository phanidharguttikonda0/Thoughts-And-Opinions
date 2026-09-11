package com.ThoughtsAndOpinions.IdentityService.gRPC;

import com.ThoughtsAndOpinions.IdentityService.model.AuthenticationResponse;
import com.ThoughtsAndOpinions.IdentityService.service.UserService;
import com.google.protobuf.Empty;
import identity.*;
import identity.IdentityGatewayServiceGrpc; // your generated proto package
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class IdentityGrpcService extends IdentityGatewayServiceGrpc.IdentityGatewayServiceImplBase {

    private final UserService service ;

    public IdentityGrpcService(UserService service) {
        this.service = service ;
    }

    @Override
    public void signUp(SignUpRequest request, StreamObserver<AuthResponse> responseObserver) {


        // need to hash the Password over here.

        AuthenticationResponse response = service.createUser(request) ;

        AuthResponse resp = AuthResponse.newBuilder()
                .setUserId(String.valueOf(response.id()))
                .setJwtToken(response.token())
                .build() ;

        responseObserver.onNext(resp); // we are sending the response

        responseObserver.onCompleted(); // telling that response has Completed
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        super.login(request, responseObserver);
    }


    @Override
    public void updateProfile(UpdateProfileRequest request, StreamObserver<Empty> responseObserver) {

    }

    @Override
    public void followUser(FollowRequest request, StreamObserver<Empty> responseObserver) {

    }



    @Override
    public void unfollowUser(FollowRequest request, StreamObserver<Empty> responseObserver) {}

    @Override
    public void getUserProfile(GetUserRequest request, StreamObserver<ProfileData> responseObserver) {
        super.getUserProfile(request, responseObserver);
    }

    @Override
    public void searchUsers(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {

    }
}