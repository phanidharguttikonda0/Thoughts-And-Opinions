package com.thoughtsandopinions.thoughtsservice.service;

import com.thoughtsandopinions.thoughtsservice.entity.ThoughtsEntity;
import com.thoughtsandopinions.thoughtsservice.entity.UsersEntity;
import com.thoughtsandopinions.thoughtsservice.exception.ThoughtNotFoundException;
import com.thoughtsandopinions.thoughtsservice.model.Thought;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtDetails;
import com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary;
import com.thoughtsandopinions.thoughtsservice.repository.ThoughtsRepository;
import com.thoughtsandopinions.thoughtsservice.utils.CursorUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ThoughtsService {

    private final ThoughtsRepository thoughtsRepo ;
    private static final Logger log = LoggerFactory.getLogger(ThoughtsService.class);

    public ThoughtsService(ThoughtsRepository thoughtsRepo){
        this.thoughtsRepo = thoughtsRepo ;
    }

    @Transactional
    public void unlikeThought(long userId, long thoughtId) {
        log.info("Attempting to unlike thoughtId: {} for userId: {}", thoughtId, userId);
        Optional<ThoughtsEntity> thought = thoughtsRepo.findById(thoughtId);

        if(thought.isEmpty()) {
            log.error("Failed to unlike: Thought not found for thoughtId: {}", thoughtId);
            throw new ThoughtNotFoundException("Invalid Thought Id");
        }

        thought.get().removeLike(userId);
        log.info("Successfully unliked thoughtId: {} for userId: {}", thoughtId, userId);
    }


    @Transactional
    public List<ThoughtDetails> getThoughtOpinions(long thoughtId, int limit, String cursor) {
        log.info("Fetching opinions for thoughtId: {}, limit: {}, cursor: {}", thoughtId, limit, cursor);
        OffsetDateTime time = null ;
        if(cursor != null && !cursor.isEmpty()){
            time = CursorUtils.decodeCursor(cursor) ;
        }

        List<ThoughtDetails> opinions = thoughtsRepo.findOpinionsByThoughtId(thoughtId, time, Pageable.ofSize(limit)) ;
        log.info("Successfully fetched {} opinions for thoughtId: {}", opinions.size(), thoughtId);
        return opinions ;
    }


    @Transactional
    public List<UserActivitySummary> getThoughtReposts(long thoughtId, int limit, String cursor) {
        log.info("Fetching reposts for thoughtId: {}, limit: {}, cursor: {}", thoughtId, limit, cursor);
        OffsetDateTime time = null ;
        if(cursor != null && !cursor.isEmpty()){
            time = CursorUtils.decodeCursor(cursor) ;
        }

        List<UserActivitySummary> repostedUsers = thoughtsRepo.findRepostedUsers(thoughtId, time, Pageable.ofSize(limit)) ;
        log.info("Successfully fetched {} reposts for thoughtId: {}", repostedUsers.size(), thoughtId);
        return repostedUsers ;
    }

    @Transactional
    public ThoughtDetails getThought(long thought_id) {
        log.info("Fetching thought details for thoughtId: {}", thought_id);
        Optional<ThoughtsEntity> thought = thoughtsRepo.findById(thought_id) ;

        if(thought.isEmpty()) {
            log.error("Failed to fetch thought: Thought not found for thoughtId: {}", thought_id);
            throw new ThoughtNotFoundException("Invalid Thought Id") ;
        }

        UsersEntity user = thought.get().getUser() ;
        log.info("Successfully fetched thought details for thoughtId: {}", thought_id);
        // soon we need to add or incorporate the media urls as well.
        Long parentThoughtId = thought.get().getParentThought() != null ? thought.get().getParentThought().getId() : null;
        return new ThoughtDetails(
                thought_id, user.getUsername(), user.getId(), user.getName(), user.getProfilePicUrl(),
                thought.get().getContent(), parentThoughtId,
                thought.get().getLikesCount() , thought.get().getOpinionsCount(),
                thought.get().getRepostsCount(), thought.get().getCreatedAt()
        ) ;

    }


    @Transactional
    public List<Thought> getUserThoughtHistory(long userId, int limit, String cursor) {
        log.info("Fetching thought history for userId: {}, limit: {}, cursor: {}", userId, limit, cursor);
        OffsetDateTime time = null ;
        if(cursor != null && !cursor.isEmpty()){
            time = CursorUtils.decodeCursor(cursor) ;
        }

        List<Thought> thoughts =  thoughtsRepo.findThoughtsOfUser(userId, time, Pageable.ofSize(limit)) ;
        log.info("Successfully fetched {} thoughts for userId: {}", thoughts.size(), userId);
        return thoughts ;
    }
}
