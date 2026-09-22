package com.thoughtsandopinions.apigateway.controller;

import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.dto.api.ThoughtsFeedDTO;
import com.thoughtsandopinions.apigateway.gRPC.TimelineServiceGrpcHandler;
import com.thoughtsandopinions.apigateway.utils.DtoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import timeline.Timeline;

@RestController
@RequestMapping("/feed")
public class TimelineController {

    private final TimelineServiceGrpcHandler timelineServiceGrpcHandler;

    public TimelineController(TimelineServiceGrpcHandler timelineServiceGrpcHandler) {
        this.timelineServiceGrpcHandler = timelineServiceGrpcHandler;
    }

    @GetMapping
    public Mono<ResponseEntity<ResponseDTO<ThoughtsFeedDTO>>> getFeed(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String cursor) {

        return Mono.fromCallable(() -> timelineServiceGrpcHandler.getFeed(userId, limit, cursor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    ThoughtsFeedDTO dto = DtoMapper.map(response);
                    return ResponseEntity.ok(
                            ResponseDTO.<ThoughtsFeedDTO>builder()
                                    .success(true)
                                    .message("Feed retrieved successfully")
                                    .data(dto)
                                    .build()
                    );
                });
    }
}
