package com.thoughtsandopinions.apigateway.controller;

import com.thoughtsandopinions.apigateway.dto.api.NotificationEvent;
import com.thoughtsandopinions.apigateway.dto.api.UsersFeedDTO;
import com.thoughtsandopinions.apigateway.dto.api.UserDTO;
import com.thoughtsandopinions.apigateway.utils.DtoMapper;
import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.gRPC.IdentityServiceGrpcHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/user")
public class FollowController {

    private final IdentityServiceGrpcHandler identityServiceGrpcHandler ;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FollowController(IdentityServiceGrpcHandler identityServiceGrpcHandler, KafkaTemplate<String, Object> kafkaTemplate) {
        this.identityServiceGrpcHandler = identityServiceGrpcHandler ;
        this.kafkaTemplate = kafkaTemplate;
    }


    @GetMapping("/{id}/follow")
    public Mono<ResponseEntity<ResponseDTO<Void>>> followUser(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @PathVariable("id") Long targetUserId) {

        return Mono.fromCallable(() -> identityServiceGrpcHandler.followUser(userId, targetUserId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(grpcResponse -> {
                    // Publish FOLLOW notification event
                    if (!userId.equals(targetUserId)) {
                        try {
                            NotificationEvent notifEvent = new NotificationEvent(
                                    "FOLLOW", userId, username != null ? username : "",
                                    targetUserId, 0L, System.currentTimeMillis()
                            );
                            kafkaTemplate.send("notification.events", String.valueOf(targetUserId), notifEvent);
                        } catch (Exception e) {
                            System.out.println("Failed to send FOLLOW notification: " + e.getMessage());
                        }
                    }

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
    public Mono<ResponseEntity<ResponseDTO<UsersFeedDTO>>> getFollowers(@PathVariable("id") Long userId,
                                                                        @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                                        @RequestParam(value = "cursor", required = false) String cursor) {

        return Mono.fromCallable(() -> identityServiceGrpcHandler.getFollowers(userId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(gRpcResponse -> {
                    java.util.List<UserDTO> users = gRpcResponse.getUsersList().stream()
                            .map(DtoMapper::map)
                            .toList();
                    UsersFeedDTO responseData = new UsersFeedDTO(users, gRpcResponse.getNextCursor()) ;

                    ResponseDTO<UsersFeedDTO> response = ResponseDTO.<UsersFeedDTO>builder()
                            .data(responseData)
                            .message("here are followers")
                            .success(true).build() ;

                    return ResponseEntity.ok(response) ;

                }) ;
    }

    @GetMapping("/{id}/followinglist")
    public Mono<ResponseEntity<ResponseDTO<UsersFeedDTO>>> getFollowings(@PathVariable("id") Long userId,
                                                                         @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                                         @RequestParam(value = "cursor", required = false) String cursor) {
        return Mono.fromCallable(() -> identityServiceGrpcHandler.getFollowing(userId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(gRpcResponse -> {
                    java.util.List<UserDTO> users = gRpcResponse.getUsersList().stream()
                            .map(DtoMapper::map)
                            .toList();
                    UsersFeedDTO responseData = new UsersFeedDTO(users, gRpcResponse.getNextCursor()) ;

                    ResponseDTO<UsersFeedDTO> response = ResponseDTO.<UsersFeedDTO>builder()
                            .data(responseData)
                            .message("here are followings")
                            .success(true).build() ;

                    return ResponseEntity.ok(response) ;

                }) ;
    }


    @GetMapping("/{id}/is-following")
    public Mono<ResponseEntity<ResponseDTO<Boolean>>> isFollowing(@RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long targetUserId) {

        return Mono.fromCallable(() -> identityServiceGrpcHandler.isFollowing(userId, targetUserId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(isFollowing -> {

                    ResponseDTO<Boolean> response = ResponseDTO.<Boolean>builder()
                            .data(isFollowing)
                            .success(true)
                            .message("successfully checked following status").build() ;

                    return ResponseEntity.status(200).body(response) ;

                }) ;
    }

}
