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
    private static final Logger log = LoggerFactory.getLogger(UsersService.class);

    public ThoughtsService(ThoughtsRepository thoughtsRepo){
        this.thoughtsRepo = thoughtsRepo ;
    }

    @Transactional
    public void unlikeThought(long userId, long thoughtId) {

        Optional<ThoughtsEntity> thought = thoughtsRepo.findById(thoughtId);

        if(thought.isEmpty()) {
            throw new ThoughtNotFoundException("Invalid Thought Id");
        }

        thought.get().removeLike(userId);

    }


    @Transactional
    public List<ThoughtDetails> getThoughtOpinions(long thoughtId, int limit, String cursor) {

        OffsetDateTime time = CursorUtils.decodeCursor(cursor) ;

        List<ThoughtDetails> opinions = thoughtsRepo.findOpinionsByThoughtId(thoughtId, time, Pageable.ofSize(limit)) ;

        return opinions ;
    }


    @Transactional
    public List<UserActivitySummary> getThoughtReposts(long thoughtId, int limit, String cursor) {

        OffsetDateTime time = CursorUtils.decodeCursor(cursor) ;

        List<UserActivitySummary> repostedUsers = thoughtsRepo.findRepostedUsers(thoughtId, time, Pageable.ofSize(limit)) ;

        return repostedUsers ;

    }

    @Transactional
    public ThoughtDetails getThought(long thought_id) {

        Optional<ThoughtsEntity> thought = thoughtsRepo.findById(thought_id) ;

        if(thought.isEmpty()) {
            throw new ThoughtNotFoundException("Invalid Thought Id") ;
        }

        UsersEntity user = thought.get().getUser() ;
        // soon we need to add or incorporate the media urls as well.
        return new ThoughtDetails(
                thought_id, user.getUsername(), user.getId(), user.getName(), user.getProfilePicUrl(),
                thought.get().getContent(), thought.get().getParentThought().getId(),
                thought.get().getLikesCount() , thought.get().getOpinionsCount(),
                thought.get().getRepostsCount(), thought.get().getCreatedAt()
        ) ;

    }


    @Transactional
    public List<Thought> getUserThoughtHistory(long userId, int limit, String cursor) {

        OffsetDateTime time = CursorUtils.decodeCursor(cursor) ;

        List<Thought> thoughts =  thoughtsRepo.findThoughtsOfUser(userId, time, Pageable.ofSize(limit)) ;

        return thoughts ;

    }
}
