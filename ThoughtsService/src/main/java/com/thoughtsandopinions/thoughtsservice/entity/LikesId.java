package com.thoughtsandopinions.thoughtsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class LikesId implements Serializable {

    @Column(name = "user_id")
    private long userId ;

    @Column(name = "thought_id")
    private long thoughtId ;

    public LikesId(long userId, long thoughtId) {
        this.userId = userId ;
        this.thoughtId = thoughtId ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LikesId that = (LikesId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(thoughtId, that.thoughtId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, thoughtId);
    }

}
