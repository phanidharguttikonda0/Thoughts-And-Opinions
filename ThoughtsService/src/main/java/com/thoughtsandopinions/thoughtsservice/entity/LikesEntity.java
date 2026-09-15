package com.thoughtsandopinions.thoughtsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "likes", schema = "thoughts_db")
public class LikesEntity implements Serializable {

    @EmbeddedId
    private LikesId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("thoughtId")
    @JoinColumn(name = "thought_id")
    private ThoughtsEntity thought ;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // mapping to the key mentioned in LikesId
    @JoinColumn(name = "user_id")
    private UsersEntity user ;

    @Column(name = "created_at")
    private OffsetDateTime createdAt; // this is set, after like count was inserted in database.

    public LikesEntity(ThoughtsEntity thought, UsersEntity user) {
        this.thought = thought;
        this.user = user;
        this.id = new LikesId(user.getId(), thought.getId()) ;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LikesEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
