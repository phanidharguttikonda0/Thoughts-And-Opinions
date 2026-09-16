package com.thoughtsandopinions.thoughtsservice.repository;


import com.thoughtsandopinions.thoughtsservice.entity.ThoughtsEntity;
import com.thoughtsandopinions.thoughtsservice.model.Thought;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtDetails;
import com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.List;

public interface ThoughtsRepository extends JpaRepository<ThoughtsEntity, Long> {

    @Query("""
    SELECT new com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary(
        u.id, u.username, u.name, u.profilePicUrl, t.createdAt
    )
    FROM ThoughtsEntity t
    JOIN t.user u
    WHERE t.parentThought.id = :thoughtId
      AND t.content IS NULL
      AND (:cursor IS NULL OR t.createdAt < :cursor)
    ORDER BY t.createdAt DESC
""")
    List<UserActivitySummary> findRepostedUsers(
            @Param("thoughtId") long thoughtId,
            @Param("cursor") OffsetDateTime cursor,
            Pageable pageable
    );


    @Query("""
    SELECT new com.thoughtsandopinions.thoughtsservice.model.Thought(
        t.id,
        t.content,
        p.id,
        t.likesCount,
        t.opinionsCount,
        t.repostsCount,
        t.createdAt
    )
    FROM ThoughtsEntity t
    LEFT JOIN t.parentThought p
    WHERE t.user.id = :userId
      AND (cast(:cursor as java.time.OffsetDateTime) IS NULL OR t.createdAt < :cursor)
    ORDER BY t.createdAt DESC
""")
    List<Thought> findThoughtsOfUser(
            @Param("userId") long userId,
            @Param("cursor") OffsetDateTime cursor,
            Pageable pageable
    );

    @Query("""
        SELECT new com.thoughtsandopinions.thoughtsservice.model.ThoughtDetails(
            t.id,
            u.username,
            u.id,
            u.name,
            u.profilePicUrl,
            t.content,
            t.parentThought.id,
            t.likesCount,
            t.opinionsCount,
            t.repostsCount,
            t.createdAt
        )
        FROM ThoughtsEntity t
        JOIN t.user u
        WHERE t.parentThought.id = :thoughtId AND t.content is not null
          AND (cast(:cursor as java.time.OffsetDateTime) IS NULL OR t.createdAt < :cursor)
        ORDER BY t.createdAt DESC
    """)
    List<ThoughtDetails> findOpinionsByThoughtId(
            @Param("thoughtId") long thoughtId,
            @Param("cursor") OffsetDateTime cursor,
            Pageable pageable
    );
}