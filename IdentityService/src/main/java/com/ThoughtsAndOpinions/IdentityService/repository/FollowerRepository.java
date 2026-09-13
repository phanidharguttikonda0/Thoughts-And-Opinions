package com.ThoughtsAndOpinions.IdentityService.repository;


import com.ThoughtsAndOpinions.IdentityService.entity.FollowerEntity;
import com.ThoughtsAndOpinions.IdentityService.entity.FollowerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FollowerRepository extends JpaRepository<FollowerEntity, FollowerId> {

    // Count how many people a user is following
    int countByFollowerId(Long followerId);

    // Count how many followers a user has
    int countByFollowingId(Long followingId);

}
