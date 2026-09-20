package com.thoughtsandopinions.apigateway.controller;

import com.thoughtsandopinions.apigateway.dto.api.FollowResponse;
import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.gRPC.IdentityServiceGrpcHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/user")
public class FollowController {

    private final IdentityServiceGrpcHandler identityServiceGrpcHandler ;

    public FollowController(IdentityServiceGrpcHandler identityServiceGrpcHandler) {
        this.identityServiceGrpcHandler = identityServiceGrpcHandler ;
    }


    @GetMapping("/{id}/follow")
    public Mono<ResponseEntity<ResponseDTO<Void>>> followUser(@RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long targetUserId) {

        return Mono.fromCallable(() -> identityServiceGrpcHandler.followUser(userId, targetUserId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(grpcResponse -> {

                    ResponseDTO<Void> response = ResponseDTO.<Void>builder()
                            .success(true)
                            .message("sucessfully followed").build() ;

                    return ResponseEntity.status(200).body(response) ;

                }) ;
    }

    @DeleteMapping("/{id}/follow")
    public Mono<ResponseEntity<ResponseDTO<Void>>> unFollowUser(@RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long targetUserId) {

        return Mono.fromCallable(() -> identityServiceGrpcHandler.unFollowUser(userId, targetUserId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(grpcResponse -> {

                    ResponseDTO<Void> response = ResponseDTO.<Void>builder()
                            .success(true)
                            .message("successfully unfollowed").build() ;

                    return ResponseEntity.status(200).body(response) ;

                }) ;
    }

    @GetMapping("/{id}/followerslist")
    public Mono<ResponseEntity<ResponseDTO<FollowResponse>>> getFollowers(@PathVariable("id") Long userId,
                                                                          @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                                          @RequestParam(value = "cursor", required = false) String cursor) {

        return Mono.fromCallable(() -> identityServiceGrpcHandler.getFollowers(userId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(gRpcResponse -> {

                    FollowResponse responseData = new FollowResponse(gRpcResponse.getUsersList(), gRpcResponse.getNextCursor()) ;

                    ResponseDTO<FollowResponse> response = ResponseDTO.<FollowResponse>builder()
                            .data(responseData)
                            .message("here are followers")
                            .success(true).build() ;

                    return ResponseEntity.ok(response) ;

                }) ;
    }

    @GetMapping("/{id}/followinglist")
    public Mono<ResponseEntity<ResponseDTO<FollowResponse>>> getFollowings(@PathVariable("id") Long userId,
                                                                           @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                                           @RequestParam(value = "cursor", required = false) String cursor) {
        return Mono.fromCallable(() -> identityServiceGrpcHandler.getFollowing(userId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(gRpcResponse -> {

                    FollowResponse responseData = new FollowResponse(gRpcResponse.getUsersList(), gRpcResponse.getNextCursor()) ;

                    ResponseDTO<FollowResponse> response = ResponseDTO.<FollowResponse>builder()
                            .data(responseData)
                            .message("here are followings")
                            .success(true).build() ;

                    return ResponseEntity.ok(response) ;

                }) ;
    }



}
