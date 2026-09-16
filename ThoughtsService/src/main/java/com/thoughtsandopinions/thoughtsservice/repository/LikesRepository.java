package com.thoughtsandopinions.thoughtsservice.repository;

import com.thoughtsandopinions.thoughtsservice.entity.LikesEntity;
import com.thoughtsandopinions.thoughtsservice.entity.LikesId;
import com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface LikesRepository extends JpaRepository<LikesEntity, LikesId> {

        @Query("""
        SELECT new com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary(
            u.id, u.username, u.name, u.profilePicUrl, l.createdAt
        )
        FROM LikesEntity l
        JOIN l.user u
        WHERE l.thought.id = :thoughtId
          AND (:cursor IS NULL OR l.createdAt < :cursor)
        ORDER BY l.createdAt DESC
    """)
        List<UserActivitySummary> findLikedUsers(
                @Param("thoughtId") long thoughtId,
                @Param("cursor") OffsetDateTime cursor,
                Pageable pageable
        );

}