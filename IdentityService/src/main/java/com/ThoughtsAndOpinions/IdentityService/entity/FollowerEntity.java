package com.ThoughtsAndOpinions.IdentityService.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "followers", schema = "identity_db")
public class FollowerEntity {

    @EmbeddedId
    private FollowerId id;

    // Maps the follower_id to the actual UserEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId") // Tells JPA this maps to the followerId inside FollowerId
    @JoinColumn(name = "follower_id")
    private UserEntity follower;

    // Maps the following_id to the actual UserEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followingId") // Tells JPA this maps to the followingId inside FollowerId
    @JoinColumn(name = "following_id")
    private UserEntity following;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public FollowerEntity() {}

    public FollowerEntity(UserEntity follower, UserEntity following) {
        this.follower = follower;
        this.following = following;
        this.id = new FollowerId(follower.getId(), following.getId());
    }

    // Getters and Setters omitted for brevity
}
