package com.ThoughtsAndOpinions.IdentityService.gRPC;

import com.ThoughtsAndOpinions.IdentityService.model.*;
import com.ThoughtsAndOpinions.IdentityService.service.UserService;
import com.ThoughtsAndOpinions.IdentityService.utils.CursorUtils;
import com.google.protobuf.Empty;
import identity.*;
import identity.IdentityGatewayServiceGrpc;
import io.grpc.stub.StreamObserver;
import jakarta.transaction.Transactional;
import org.springframework.grpc.server.service.GrpcService;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;

@GrpcService
public class IdentityGrpcService extends IdentityGatewayServiceGrpc.IdentityGatewayServiceImplBase {

    private final UserService service ;

    public IdentityGrpcService(UserService service) {
        this.service = service ;
    }

    @Override
    public void signUp(SignUpRequest request, StreamObserver<AuthResponse> responseObserver) {

        AuthenticationResponse response = service.createUser(request) ;

        AuthResponse resp = AuthResponse.newBuilder()
                .setUserId(response.id())
                .setJwtToken(response.token())
                .build() ;

        responseObserver.onNext(resp); // we are sending the response

        responseObserver.onCompleted(); // telling that response has Completed
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {

        AuthenticationResponse response = service.authenticateUser(request.getUsername(), request.getPassword()) ;

        AuthResponse resp = AuthResponse.newBuilder()
                .setUserId(response.id()) // we need to make sure to change this to Long
                .setJwtToken(response.token())
                .build() ;

        responseObserver.onNext(resp);
        responseObserver.onCompleted();

    }


    @Override
    public void updateProfile(UpdateProfileRequest request, StreamObserver<Empty> responseObserver) {
        /*
        * As we specified optional in proto buffers, but it will send us an empty string instead
        * of null value, so we need to check , using the has function.
        * */

        // Convert proto optional wrappers into standard Java Optionals
        Optional<String> username = request.hasUsername() ? Optional.of(request.getUsername()) : Optional.empty();
        Optional<String> name = request.hasName() ? Optional.of(request.getName()) : Optional.empty();
        Optional<String> bio = request.hasBio() ? Optional.of(request.getBio()) : Optional.empty();

        // Note: Map proto's 'avatar_url' to your service's 'profilePicUrl' parameter
        Optional<String> profilePicUrl = request.hasAvatarUrl() ? Optional.of(request.getAvatarUrl()) : Optional.empty();


        service.updateProfile(request.getUserId(), username, name, bio, profilePicUrl);

        responseObserver.onNext(Empty.getDefaultInstance()); // means the update was successfull
        responseObserver.onCompleted();
    }

    @Override
    public void followUser(FollowRequest request, StreamObserver<Empty> responseObserver) {

        service.followUser(request.getUserId(), request.getTargetUserId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }



    @Override
    public void unfollowUser(FollowRequest request, StreamObserver<Empty> responseObserver) {

        service.unfollowUser(request.getUserId(), request.getTargetUserId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getUserProfile(GetUserRequest request, StreamObserver<ProfileData> responseObserver) {

        Profile profile = service.getUserProfile(request.getUserId());

        // Converted OffsetDateTime to com.google.protobuf.Timestamp, as proto buffers cannot understand
        // standard java templates
        java.time.Instant instant = profile.createdAt().toInstant();
        com.google.protobuf.Timestamp joinedAtTimestamp = com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        ProfileData resp = ProfileData.newBuilder()
                .setUserId(profile.userId())
                .setBio(profile.bio())
                .setName(profile.name())
                .setUsername(profile.name())
                .setFollowersCount(profile.followersCount())
                .setFollowingCount(profile.followingCount())
                .setProfilePic(profile.profilePicUrl())
                .setJoinedAt(joinedAtTimestamp)
                .build() ;

        responseObserver.onNext(resp);
        responseObserver.onCompleted();

    }

    @Override
    public void searchUsers(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
           ArrayList<ProfileDetails> profileDetails =  service.getSearchedProfile(request.getQuery());

        SearchResponse.Builder resp = SearchResponse.newBuilder() ;
           for (ProfileDetails profile : profileDetails) {
               resp.addUsers(UserDetails.newBuilder()
                       .setUserId(profile.userId())
                       .setName(profile.name())
                       .setUsername(profile.username())
                       .setProfilePic(profile.profilePicUrl())
                       .build()) ;
           }

           SearchResponse response = resp.build() ;

           responseObserver.onNext(response);
           responseObserver.onCompleted();

    }

    // need to implement get followersList , get followingList using cursor pagination
    // and need to mention those 2 functionalities in the .proto file

    @Override
    public void getFollowersList(UsersListRequest request, StreamObserver<UsersListResponse> responseObserver) {

        ArrayList<ProfileDetails> profileDetails = service.getFollowersList(request.getUserId(), request.getLimit(), request.getCursor()) ;

        UsersListResponse.Builder resp = UsersListResponse.newBuilder() ;
        int index = 0 ;
        OffsetDateTime lastCursor = null ;
        for (ProfileDetails profile : profileDetails) {
            resp.addUsers(UserDetails.newBuilder()
                    .setUserId(profile.userId())
                    .setName(profile.name())
                    .setUsername(profile.username())
                    .setProfilePic(profile.profilePicUrl())
                    .build()) ;
            index += 1 ;
            if (index == request.getLimit()) {
                lastCursor = profile.createdAt() ;
            }
        }
        resp.setNextCursor(CursorUtils.encodeCursor(lastCursor)) ;
        UsersListResponse response = resp.build() ;


        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFollowingList(UsersListRequest request, StreamObserver<UsersListResponse> responseObserver) {
        ArrayList<ProfileDetails> profileDetails = service.getFollowingList(request.getUserId(), request.getLimit(), request.getCursor()) ;

        UsersListResponse.Builder resp = UsersListResponse.newBuilder() ;
        int index = 0 ;
        OffsetDateTime lastCursor = null ;
        for (ProfileDetails profile : profileDetails) {
            resp.addUsers(UserDetails.newBuilder()
                    .setUserId(profile.userId())
                    .setName(profile.name())
                    .setUsername(profile.username())
                    .setProfilePic(profile.profilePicUrl())
                    .build()) ;
            index += 1 ;
            if (index == request.getLimit()) {
                lastCursor = profile.createdAt() ;
            }
        }
        resp.setNextCursor(CursorUtils.encodeCursor(lastCursor)) ;
        UsersListResponse response = resp.build() ;


        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}