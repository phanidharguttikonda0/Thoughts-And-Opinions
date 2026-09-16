package com.thoughtsandopinions.thoughtsservice.gRPC;

import com.google.protobuf.Empty;
import com.thoughtsandopinions.thoughtsservice.service.UsersService;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import thoughts.ThoughtGatewayServiceGrpc;
import thoughts.Thoughts;

@GrpcService
public class ThoughtsGrpcService extends ThoughtGatewayServiceGrpc.ThoughtGatewayServiceImplBase {

    private final UsersService usersService ;
    private static final Logger log = LoggerFactory.getLogger(ThoughtsGrpcService.class);

    public ThoughtsGrpcService(UsersService usersService) {
        this.usersService = usersService ;
    }


    @Override
    public void createThought(Thoughts.CreateRequest request, StreamObserver<Thoughts.CreateResponse> responseObserver) {

    }

    @Override
    public void deleteThought(Thoughts.DeleteRequest request, StreamObserver<Empty> responseObserver) {

    }

    @Override
    public void likeThought(Thoughts.LikeRequest request, StreamObserver<Empty> responseObserver) {

    }

    @Override
    public void unlikeThought(Thoughts.LikeRequest request, StreamObserver<Empty> responseObserver) {

    }

    @Override
    public void getThoughtLikes(Thoughts.GetLikesRequest request, StreamObserver<Thoughts.GetLikesResponse> responseObserver) {}

    @Override
    public void getOpinions(Thoughts.GetOpinionsRequest request, StreamObserver<Thoughts.GetOpinionsResponse> responseObserver) {}


    @Override
    public void getUserHistory(Thoughts.GetUserHistoryRequest request, StreamObserver<Thoughts.GetHistoryResponse> responseObserver) {}

    @Override
    public void getThought(Thoughts.GetThoughtRequest request, StreamObserver<Thoughts.GetThoughtResponse> responseObserver) {}

    @Override
    public void getReposts(Thoughts.GetRepostsRequest request, StreamObserver<Thoughts.GetRepostsResponse> responseObserver) {
        // on a particular thought , who ever reposted we are going to return that users list
    }

}
