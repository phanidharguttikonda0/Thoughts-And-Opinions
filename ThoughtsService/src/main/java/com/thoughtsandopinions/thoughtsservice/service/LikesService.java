package com.thoughtsandopinions.thoughtsservice.service;


import com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary;
import com.thoughtsandopinions.thoughtsservice.repository.LikesRepository;
import com.thoughtsandopinions.thoughtsservice.utils.CursorUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LikesService {

    private final LikesRepository repo;
    private static final Logger log = LoggerFactory.getLogger(LikesService.class);

    public LikesService(LikesRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public List<UserActivitySummary> getThoughtLikes(long thoughtId, int limit, String cursor) {

        log.info("Fetching likes for thoughtId: {}, limit: {}, cursor: {}", thoughtId, limit, cursor);
        OffsetDateTime time = null ;
        if(cursor != null && !cursor.isEmpty()){
            time = CursorUtils.decodeCursor(cursor) ;
        }
        List<UserActivitySummary> likedUsers = repo.findLikedUsers(thoughtId, time, Pageable.ofSize(limit)) ;
        log.info("Successfully fetched {} likes for thoughtId: {}", likedUsers.size(), thoughtId);

        return likedUsers ;
    }
}
