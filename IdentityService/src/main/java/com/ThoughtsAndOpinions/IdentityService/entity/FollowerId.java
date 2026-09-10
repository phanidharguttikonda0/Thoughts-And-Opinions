package com.ThoughtsAndOpinions.IdentityService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/*
* In JPA, when a table has a composite primary key (follower_id and following_id), you must define those keys in a separate
* class annotated with @Embeddable. This class must implement Serializable and override equals and hashCode so Hibernate can
* compare the keys in memory.
* */
@Getter
@Setter
@Embeddable
public class FollowerId implements Serializable {

    @Column(name = "follower_id")
    private Long followerId;

    @Column(name = "following_id")
    private Long followingId;

    // Required by JPA: no-args constructor
    public FollowerId() {}

    public FollowerId(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    // Getters and Setters
    public Long getFollowerId() { return followerId; }
    public void setFollowerId(Long followerId) { this.followerId = followerId; }
    public Long getFollowingId() { return followingId; }
    public void setFollowingId(Long followingId) { this.followingId = followingId; }

    // equals and hashCode are mandatory for composite keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FollowerId that = (FollowerId) o;
        return Objects.equals(followerId, that.followerId) &&
                Objects.equals(followingId, that.followingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followingId);
    }
}
