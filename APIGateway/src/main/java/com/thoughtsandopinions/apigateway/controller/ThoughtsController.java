package com.thoughtsandopinions.apigateway.controller;


import com.thoughtsandopinions.apigateway.dto.api.CreateThoughtDTO;
import com.thoughtsandopinions.apigateway.dto.api.*;
import com.thoughtsandopinions.apigateway.utils.DtoMapper;
import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.dto.api.ThoughtDetailsDTO;
import com.thoughtsandopinions.apigateway.gRPC.ThoughtsServiceGrpcHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import thoughts.Thoughts;

@RestController
@RequestMapping("/thoughts")
public class ThoughtsController {

    private final ThoughtsServiceGrpcHandler thoughtsServiceGrpcHandler ;

    public ThoughtsController(ThoughtsServiceGrpcHandler thoughtsServiceGrpcHandler) {
        this.thoughtsServiceGrpcHandler = thoughtsServiceGrpcHandler ;
    }


    @PostMapping("/") // creates the thought
    public Mono<ResponseEntity<ResponseDTO<CreateThoughtResponseDTO>>> createThought(@RequestHeader("X-User-Id") Long userId, @RequestBody CreateThoughtDTO request) {

        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.createThought(userId, request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    CreateThoughtResponseDTO dto = DtoMapper.map(response);
                    return ResponseEntity.ok(
                        ResponseDTO.<CreateThoughtResponseDTO>builder()
                                .success(true)
                                .message("successfully created thought")
                                .data(dto)
                                .build()
                    );
                }) ;
    }

    @DeleteMapping("/{thoughtId}") // deletes the thought
    public Mono<ResponseEntity<ResponseDTO<Void>>> deleteThought(@RequestHeader("X-User-Id") Long userId, @PathVariable("thoughtId") Long thoughtId) {
        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.deleteThought(userId, thoughtId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.ok(
                        ResponseDTO.<Void>builder()
                                .success(true)
                                .message("successfully deleted thought")
                                .build()
                )) ;
    }

    @GetMapping("/interact/{thoughtId}")
    public Mono<ResponseEntity<ResponseDTO<Void>>> likeThought(@RequestHeader("X-User-Id") Long userId, @PathVariable("thoughtId") Long thoughtId) {

        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.likeThought(userId, thoughtId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.ok(
                        ResponseDTO.<Void>builder()
                                .success(true)
                                .message("liked thought")
                                .build()
                )) ;
    }

    @DeleteMapping("/interact/{thoughtId}")
    public Mono<ResponseEntity<ResponseDTO<Void>>> unlikeThought(@RequestHeader("X-User-Id") Long userId, @PathVariable("thoughtId") Long thoughtId) {
        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.unLikeThought(userId, thoughtId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.ok(
                        ResponseDTO.<Void>builder()
                                .success(true)
                                .message("unliked thought")
                                .build()
                )) ;
    }


    @GetMapping("/likes/{thoughtId}")
    public Mono<ResponseEntity<ResponseDTO<UsersFeedDTO>>> getThoughtLikes(@PathVariable("thoughtId") Long thoughtId,
                                                                           @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                                           @RequestParam(value = "cursor", required = false) String cursor) {
        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.getThoughtLikes(thoughtId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    java.util.List<UserDTO> users = response.getLikedUsersList().stream()
                            .map(DtoMapper::map)
                            .toList();
                    UsersFeedDTO likedUsers = new UsersFeedDTO(users, response.getNextCursor()) ;

                    return ResponseEntity.ok(ResponseDTO.<UsersFeedDTO>builder()
                            .success(true).message("got liked users list")
                            .data(likedUsers).build()
                    ) ;

                });
    }

    @GetMapping("/reposts/{thoughtId}")
    public Mono<ResponseEntity<ResponseDTO<UsersFeedDTO>>> getThoughtReposts(@PathVariable("thoughtId") Long thoughtId,
                                                                             @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                                             @RequestParam(value = "cursor", required = false) String cursor) {
        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.getThoughtReposts(thoughtId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    java.util.List<UserDTO> users = response.getRepostedUsersListList().stream()
                            .map(DtoMapper::map)
                            .toList();
                    UsersFeedDTO repostedUsers = new UsersFeedDTO(users, response.getNextCursor()) ;

                    return ResponseEntity.ok(ResponseDTO.<UsersFeedDTO>builder()
                            .success(true).message("got reposted users list")
                            .data(repostedUsers).build()
                    ) ;

                });
    }

    @GetMapping("/{thoughtId}") // returns the Thought
    public Mono<ResponseEntity<ResponseDTO<ThoughtDetailsDTO>>> getThought(@PathVariable("thoughtId") Long thoughtId) {
        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.getThought(thoughtId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {

                    ThoughtDetailsDTO thoughtDetails = DtoMapper.map(response);

                    return ResponseEntity.ok(ResponseDTO.<ThoughtDetailsDTO>builder()
                            .success(true)
                            .message("got the thought")
                            .data(thoughtDetails)
                            .build()
                    );

                }) ;
    }

    @GetMapping("/opinions/{thoughtId}")
    public Mono<ResponseEntity<ResponseDTO<ThoughtsFeedDTO>>> getOpinions(@PathVariable("thoughtId") Long thoughtId,
                                                                          @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                                          @RequestParam(value = "cursor", required = false) String cursor) {
        return Mono.fromCallable(() -> thoughtsServiceGrpcHandler.getOpinions(thoughtId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    java.util.List<ThoughtDetailsDTO> opinionsList = response.getOpinionsListList().stream()
                            .map(DtoMapper::map)
                            .toList();
                    ThoughtsFeedDTO feed = new ThoughtsFeedDTO(opinionsList, response.getNextCursor());

                    return ResponseEntity.ok(ResponseDTO.<ThoughtsFeedDTO>builder()
                            .success(true)
                            .message("got opinions")
                            .data(feed)
                            .build()
                    );
                }) ;
    }
}
