package com.thoughtsandopinions.timelineservice.consumer;

import com.thoughtsandopinions.timelineservice.dto.ThoughtCreatedEvent;
import com.thoughtsandopinions.timelineservice.grpc.IdentityServiceGrpcHandler;
import identity.UserDetails;
import identity.UsersListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineKafkaConsumer {

    private final StringRedisTemplate redisTemplate;
    private final IdentityServiceGrpcHandler identityServiceGrpcHandler;

    @KafkaListener(topics = "thought.created", groupId = "timeline-service-group-5")
    public void consumeThoughtCreatedEvent(ThoughtCreatedEvent event) {
        log.info("Received ThoughtCreatedEvent: {}", event);
        try {
            Long authorId = Long.parseLong(event.authorId());
            String cursor = "";
            int limit = 100;

            // we are going fetch the followers of that user and appending the thoughtID into their feed
            while (true) {
                UsersListResponse response = identityServiceGrpcHandler.getFollowersList(authorId, limit, cursor);

                if (response == null || response.getUsersList().isEmpty()) {
                    break;
                }

                // Iterate through the followers list
                for (UserDetails user : response.getUsersList()) {
                    String feedKey = "feed:" + user.getUserId();
                    
                    // Append the thought to the user's feed ZSET
                    // The score is the createdAt timestamp for chronological orde  ring
                    redisTemplate.opsForZSet().add(feedKey, event.thoughtId(), event.createdAt());
                    
                    // When the size has increased to 120 or more, we remove the last 20 items (keeping only 100)
                    Long size = redisTemplate.opsForZSet().zCard(feedKey);
                    if (size != null && size >= 120) {
                        // removeRange(0, -101) deletes elements from rank 0 (oldest) to rank -101 (100th from latest).
                        // This effectively keeps exactly the 100 most recent items in the ZSET.
                        redisTemplate.opsForZSet().removeRange(feedKey, 0, -101);
                        log.debug("Trimmed timeline for user {}", user.getUserId());
                    }
                }

                cursor = response.getNextCursor();
                if (cursor == null || cursor.isEmpty()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to process ThoughtCreatedEvent", e);
        }
    }
}
