package com.ThoughtsAndOpinions.IdentityService.repository;


import com.ThoughtsAndOpinions.IdentityService.entity.UserEntity;
import com.ThoughtsAndOpinions.IdentityService.model.ProfileDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);

    // Maps to your "LIKE username%" SearchRequest requirement
    List<UserEntity> findTop5ByUsernameStartingWithIgnoreCase(String prefix);

    @Query("SELECT new com.ThoughtsAndOpinions.IdentityService.model.ProfileDetails(u.id, u.username, u.name, u.profilePicUrl, u.createdAt) " +
            "FROM UserEntity u " +
            "WHERE u.username LIKE :usernamePrefix%")
    List<ProfileDetails> findTop5UsersByUsernameStartingWith(
            @Param("usernamePrefix") String usernamePrefix,
            PageRequest pageable
    );

// inside FollowerRepository

    @Query("SELECT new com.ThoughtsAndOpinions.IdentityService.model.ProfileDetails(" +
            "f.follower.id, f.follower.username, f.follower.name, f.follower.profilePicUrl, f.createdAt) " + // <-- Select f.createdAt
            "FROM FollowerEntity f " +
            "WHERE f.following.id = :userId " +
            "AND f.createdAt <= :cursor " +
            "ORDER BY f.createdAt DESC")
    List<ProfileDetails> getFollowersList(
            @Param("cursor") OffsetDateTime cursor,
            @Param("userId") long userId,
            PageRequest pageable
    );

    @Query("SELECT new com.ThoughtsAndOpinions.IdentityService.model.ProfileDetails(" +
            "f.following.id, f.following.username, f.following.name, f.following.profilePicUrl, f.createdAt) " + // <-- Select f.createdAt
            "FROM FollowerEntity f " +
            "WHERE f.follower.id = :userId " +
            "AND f.createdAt <= :cursor " +
            "ORDER BY f.createdAt DESC")
    List<ProfileDetails> getFollowingList(
            @Param("cursor") OffsetDateTime cursor,
            @Param("userId") long userId,
            PageRequest pageable
    );

}
