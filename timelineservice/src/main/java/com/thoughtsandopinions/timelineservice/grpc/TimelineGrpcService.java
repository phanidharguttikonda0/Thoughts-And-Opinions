package com.thoughtsandopinions.timelineservice.grpc;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.grpc.server.service.GrpcService;
import thoughts.Thoughts.GetThoughtResponse;
import timeline.Timeline;
import timeline.TimelineGatewayServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.Set;

@GrpcService
public class TimelineGrpcService extends TimelineGatewayServiceGrpc.TimelineGatewayServiceImplBase {
    private final ThoughtsServiceGrpcHandler thoughtGatewayServiceGrpcHandler;
    private final StringRedisTemplate redisTemplate;

    public TimelineGrpcService(ThoughtsServiceGrpcHandler thoughtGatewayServiceGrpcHandler, StringRedisTemplate redisTemplate) {
        this.thoughtGatewayServiceGrpcHandler = thoughtGatewayServiceGrpcHandler;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void getFeed(Timeline.GetFeedRequest request, StreamObserver<Timeline.GetFeedResponse> responseObserver) {
        long userId = request.getUserId();
        int limit = request.getLimit() > 0 ? request.getLimit() : 10;
        String cursor = request.hasCursor() ? request.getCursor() : null;

        String key = "feed:" + userId;
        Set<TypedTuple<String>> feedItems;

        if (cursor != null && !cursor.isEmpty()) {
            double maxScore = Double.parseDouble(cursor) - 1; // get items strictly older than the cursor score
            feedItems = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, maxScore, 0, limit);
        } else {
            feedItems = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        }

        Timeline.GetFeedResponse.Builder responseBuilder = Timeline.GetFeedResponse.newBuilder();
        String nextCursor = "";

        // we are iterating over the thought ids we got and then we are getting the
        // each thought detail from the thoughts service
        if (feedItems != null && !feedItems.isEmpty()) {
            for (TypedTuple<String> item : feedItems) {
                String thoughtIdStr = item.getValue();
                if (thoughtIdStr != null) {
                    try {
                        Long thoughtId = Long.parseLong(thoughtIdStr);
                        GetThoughtResponse thoughtResponse = thoughtGatewayServiceGrpcHandler.getThought(thoughtId);
                        if (thoughtResponse != null) {
                            responseBuilder.addThoughtsList(thoughtResponse);
                        }
                    } catch (Exception e) {
                        // Ignore or log error
                    }
                }
                nextCursor = String.valueOf(item.getScore().longValue());
            }
        }

        if (!nextCursor.isEmpty()) {
            responseBuilder.setNextCursor(nextCursor);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
