package com.thoughtsandopinions.thoughtsservice.gRPC;

import com.google.protobuf.Empty;
import com.thoughtsandopinions.thoughtsservice.model.Thought;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtDetails;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtsResponse;
import com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary;
import com.thoughtsandopinions.thoughtsservice.service.LikesService;
import com.thoughtsandopinions.thoughtsservice.service.ThoughtsService;
import com.thoughtsandopinions.thoughtsservice.service.UsersService;
import com.thoughtsandopinions.thoughtsservice.utils.CursorUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import thoughts.ThoughtGatewayServiceGrpc;
import thoughts.Thoughts;

import java.util.List;

@GrpcService
public class ThoughtsGrpcService extends ThoughtGatewayServiceGrpc.ThoughtGatewayServiceImplBase {

    private final UsersService usersService ;
    private final LikesService likesService ;
    private final ThoughtsService thoughtsService ;
    private static final Logger log = LoggerFactory.getLogger(ThoughtsGrpcService.class);

    public ThoughtsGrpcService(UsersService usersService, ThoughtsService thoughtsService, LikesService likesService) {
        this.usersService = usersService ;
        this.thoughtsService = thoughtsService ;
        this.likesService = likesService ;
    }


    @Override
    public void createThought(Thoughts.CreateRequest request, StreamObserver<Thoughts.CreateResponse> responseObserver) {

        ThoughtsResponse response = usersService.createThought(request) ;

        java.time.Instant instant = response.createdAt().toInstant();
        com.google.protobuf.Timestamp joinedAtTimestamp = com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        Thoughts.CreateResponse resp = Thoughts.CreateResponse.newBuilder()
                .setThoughtId(response.thoughtId())
                .setCreatedAt(joinedAtTimestamp)
                .build() ;

        responseObserver.onNext(resp);
        responseObserver.onCompleted();

    }

    @Override
    public void deleteThought(Thoughts.DeleteRequest request, StreamObserver<Empty> responseObserver) {

        usersService.deleteThought(request);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();

    }

    @Override
    public void likeThought(Thoughts.LikeRequest request, StreamObserver<Empty> responseObserver) {

        log.info("got the like thought instance");

        usersService.likeThought(request.getUserId(), request.getThoughtId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();

    }

    @Override
    public void unlikeThought(Thoughts.LikeRequest request, StreamObserver<Empty> responseObserver) {

        thoughtsService.unlikeThought(request.getUserId(), request.getThoughtId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getThoughtLikes(Thoughts.GetLikesRequest request, StreamObserver<Thoughts.GetLikesResponse> responseObserver) {

        List<UserActivitySummary> likedUsers = likesService.getThoughtLikes(request.getThoughtId(), request.getLimit(), request.getCursor()) ;

        Thoughts.GetLikesResponse.Builder resp = Thoughts.GetLikesResponse.newBuilder() ;
        int i = 0 ;
        String lastCursor = null;
        for (UserActivitySummary activity : likedUsers) {
            Thoughts.Users.Builder userBuilder = Thoughts.Users.newBuilder()
                    .setUserId(activity.getUserId())
                    .setUsername(activity.getUsername())
                    .setName(activity.getName());
            if (activity.getProfilePicUrl() != null) {
                userBuilder.setProfilePicUrl(activity.getProfilePicUrl());
            }
            resp.addLikedUsers(userBuilder) ;
            if (i == likedUsers.size()-1 && activity.getActivityCreatedAt() != null) {
                lastCursor = CursorUtils.encodeCursor(activity.getActivityCreatedAt()) ;
            }
            i += 1 ;
        }

        if (lastCursor != null) {
            resp.setNextCursor(lastCursor) ;
        }

        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();

    }

    @Override
    public void storeUser(Thoughts.Users request, StreamObserver<Empty> responseObserver) {
        usersService.storeUser(request);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getOpinions(Thoughts.GetOpinionsRequest request, StreamObserver<Thoughts.GetOpinionsResponse> responseObserver) {

        List<ThoughtDetails> opinions = thoughtsService.getThoughtOpinions(request.getThoughtId(), request.getLimit(), request.getCursor()) ;

        Thoughts.GetOpinionsResponse.Builder resp = Thoughts.GetOpinionsResponse.newBuilder() ;

        int i = 0 ;
        String lastCursor = null ;

        for(ThoughtDetails t : opinions) {

            Thoughts.Users.Builder userBuilder = Thoughts.Users.newBuilder()
                    .setUserId(t.getUserId())
                    .setUsername(t.getUsername())
                    .setName(t.getName());
            if (t.getProfilePicUrl() != null) {
                userBuilder.setProfilePicUrl(t.getProfilePicUrl());
            }

            Thoughts.GetThoughtResponse.Builder opinionBuilder = Thoughts.GetThoughtResponse.newBuilder()
                    .setUser(userBuilder.build())
                    .setThoughtId(t.getThoughtId())
                    .setLikesCount(t.getLikesCount())
                    .setOpinionsCount(t.getOpinionsCount())
                    .setRepostsCount(t.getRepostsCount());

            if (t.getContent() != null) {
                opinionBuilder.setContent(t.getContent());
            }
            if (t.getParentThoughtId() != null) {
                opinionBuilder.setParentThoughtId(t.getParentThoughtId());
            }
            if (t.getCreatedAt() != null) {
                java.time.Instant instant = t.getCreatedAt().toInstant();
                com.google.protobuf.Timestamp joinedAtTimestamp = com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build();
                opinionBuilder.setCreatedAt(joinedAtTimestamp);
            }

            resp.addOpinionsList(opinionBuilder);

            if (i == opinions.size()-1 && t.getCreatedAt() != null) {
                lastCursor = CursorUtils.encodeCursor(t.getCreatedAt()) ;
            }

            i += 1 ;
        }

        if (lastCursor != null) {
            resp.setNextCursor(lastCursor) ;
        }

        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }


    @Override
    public void getUserHistory(Thoughts.GetUserHistoryRequest request, StreamObserver<Thoughts.GetHistoryResponse> responseObserver) {

        List<Thought> thoughts = thoughtsService.getUserThoughtHistory(request.getUserId(), request.getLimit(), request.getCursor()) ;

        Thoughts.GetHistoryResponse.Builder resp = Thoughts.GetHistoryResponse.newBuilder() ;


        int i = 0 ;
        String lastCursor = null ;

        for(Thought t : thoughts) {

            Thoughts.ThoughtDetails.Builder thoughtBuilder = Thoughts.ThoughtDetails.newBuilder()
                    .setThoughtId(t.getThoughtId())
                    .setLikesCount(t.getLikesCount())
                    .setRepostsCount(t.getRepostsCount())
                    .setOpinionsCount(t.getOpinionsCount());

            if (t.getContent() != null) {
                thoughtBuilder.setContent(t.getContent());
            }
            if (t.getParentThoughtId() != null) {
                thoughtBuilder.setParentThoughtId(t.getParentThoughtId());
            }
            if (t.getCreatedAt() != null) {
                java.time.Instant instant = t.getCreatedAt().toInstant();
                com.google.protobuf.Timestamp joinedAtTimestamp = com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build();
                thoughtBuilder.setCreatedAt(joinedAtTimestamp);
            }

            if (request.hasLoggedInUserId()) {
                long loggedInUserId = request.getLoggedInUserId();
                thoughtBuilder.setIsLiked(thoughtsService.isLikedByUser(loggedInUserId, t.getThoughtId()));
                thoughtBuilder.setIsReposted(thoughtsService.isRepostedByUser(loggedInUserId, t.getThoughtId()));
            }

            resp.addThoughtsList(thoughtBuilder);

            if(i == thoughts.size()-1 && t.getCreatedAt() != null) {
                lastCursor = CursorUtils.encodeCursor(t.getCreatedAt()) ;
            }
            i += 1 ;
        }

        if (lastCursor != null) {
            resp.setNextCursor(lastCursor) ;
        }

        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();

    }

    @Override
    public void getThought(Thoughts.GetThoughtRequest request, StreamObserver<Thoughts.GetThoughtResponse> responseObserver) {

        ThoughtDetails thought = thoughtsService.getThought(request.getThoughtId()) ;

        Thoughts.GetThoughtResponse.Builder resp = Thoughts.GetThoughtResponse.newBuilder() ;

        Thoughts.Users.Builder userBuilder = Thoughts.Users.newBuilder()
                .setUserId(thought.getUserId())
                .setUsername(thought.getUsername())
                .setName(thought.getName());
        if (thought.getProfilePicUrl() != null) {
            userBuilder.setProfilePicUrl(thought.getProfilePicUrl());
        }
        resp.setUser(userBuilder.build());

        resp.setThoughtId(thought.getThoughtId());
        resp.setLikesCount(thought.getLikesCount()) ;
        resp.setOpinionsCount(thought.getOpinionsCount()) ;
        resp.setRepostsCount(thought.getRepostsCount()) ;

        if (thought.getContent() != null) {
            resp.setContent(thought.getContent()) ;
        }
        if (thought.getParentThoughtId() != null) {
            resp.setParentThoughtId(thought.getParentThoughtId());
        }

        if (thought.getCreatedAt() != null) {
            java.time.Instant instant = thought.getCreatedAt().toInstant();
            com.google.protobuf.Timestamp joinedAtTimestamp = com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
            resp.setCreatedAt(joinedAtTimestamp) ;
        }

        if (request.hasUserId()) {
            long userId = request.getUserId();
            resp.setIsLiked(thoughtsService.isLikedByUser(userId, thought.getThoughtId()));
            resp.setIsReposted(thoughtsService.isRepostedByUser(userId, thought.getThoughtId()));
        }

        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();

    }

    @Override
    public void getReposts(Thoughts.GetRepostsRequest request, StreamObserver<Thoughts.GetRepostsResponse> responseObserver) {
        // on a particular thought , who ever reposted we are going to return that users list
        List<UserActivitySummary> repostedUsers = thoughtsService.getThoughtReposts(request.getThoughtId(), request.getLimit(), request.getCursor()) ;

        Thoughts.GetRepostsResponse.Builder response = Thoughts.GetRepostsResponse.newBuilder() ;

        int i = 0 ;
        String lastCursor = null ;
        for(UserActivitySummary activity : repostedUsers) {

            Thoughts.Users.Builder userBuilder = Thoughts.Users.newBuilder()
                    .setUserId(activity.getUserId())
                    .setUsername(activity.getUsername())
                    .setName(activity.getName());
            if (activity.getProfilePicUrl() != null) {
                userBuilder.setProfilePicUrl(activity.getProfilePicUrl());
            }
            response.addRepostedUsersList(userBuilder) ;
            if (i == repostedUsers.size()-1 && activity.getActivityCreatedAt() != null) {
                lastCursor = CursorUtils.encodeCursor(activity.getActivityCreatedAt()) ;
            }
            i += 1 ;
        }

        if (lastCursor != null) {
            response.setNextCursor(lastCursor) ;
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();

    }

}
