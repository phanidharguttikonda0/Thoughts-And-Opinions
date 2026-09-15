package com.ThoughtsAndOpinions.IdentityService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@Embeddable
public class FollowerId implements Serializable {

    @Column(name = "follower_id")
    private Long followerId;

    @Column(name = "following_id")
    private Long followingId;

    public FollowerId(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

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
